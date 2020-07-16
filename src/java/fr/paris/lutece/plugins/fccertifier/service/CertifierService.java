/*
 * Copyright (c) 2002-2017, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */

package fr.paris.lutece.plugins.fccertifier.service;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import fr.paris.lutece.plugins.fccertifier.business.FcIdentity;
import fr.paris.lutece.plugins.identitystore.v2.web.rs.dto.AttributeDto;
import fr.paris.lutece.plugins.identitystore.v2.web.rs.dto.AuthorDto;
import fr.paris.lutece.plugins.identitystore.v2.web.rs.dto.CertificateDto;
import fr.paris.lutece.plugins.identitystore.v2.web.rs.dto.IdentityChangeDto;
import fr.paris.lutece.plugins.identitystore.v2.web.rs.dto.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v2.web.service.AuthorType;
import fr.paris.lutece.plugins.identitystore.v2.web.service.IdentityService;
import fr.paris.lutece.plugins.oauth2.modules.franceconnect.business.UserInfo;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

/**
 * FranceConnect Certifier Service
 */
public class CertifierService implements Serializable
{
    private static final String MESSAGE_CODE_VALIDATION_OK = "module.identitystore.fccertifier.message.validation.ok";
    private static final String MESSAGE_CODE_VALIDATION_INVALID = "module.identitystore.fccertifier.message.validation.invalidCode";
    private static final String MESSAGE_SESSION_EXPIRED = "module.identitystore.fccertifier.message.validation.sessionExpired";
    private static final String MESSAGE_CODE_EXPIRED = "module.identitystore.fccertifier.message.validation.codeExpired";
    private static final String MESSAGE_TOO_MANY_ATTEMPS = "module.identitystore.fccertifier.message.validation.tooManyAttempts";
    private static final String PROPERTY_EXPIRES_DELAY = "identitystore.fccertifier.expiresDelay";
    private static final String PROPERTY_MOCKED_EMAIL = "identitystore.fccertifier.mockedEmail";
    private static final String PROPERTY_MOCKED_CONNECTION_ID = "identitystore.fccertifier.mockedConnectionId";
    private static final String DEFAULT_CONNECTION_ID = "1";
    private static final String DEFAULT_EMAIL = "test@test.fr";
    private static final int DEFAULT_EXPIRES_DELAY = 5;
    private static final String MOCKED_USER_CONNECTION_ID = AppPropertiesService.getProperty( PROPERTY_MOCKED_CONNECTION_ID, DEFAULT_CONNECTION_ID );
    private static final String MOCKED_USER_EMAIL = AppPropertiesService.getProperty( PROPERTY_MOCKED_EMAIL, DEFAULT_EMAIL );
    private static final int EXPIRES_DELAY = AppPropertiesService.getPropertyInt( PROPERTY_EXPIRES_DELAY, DEFAULT_EXPIRES_DELAY );
    private static final String BEAN_IDENTITYSTORE_SERVICE = "fccertifier.identitystore.service";
    private static final String PROPERTY_IDENTITY_SERVICE_CLIENT_CODE = "fccertifier.identitystore.client.code";
    private static final String CERTIFIER_CODE = "fccertifier";
    private static final String CLIENT_CODE = AppPropertiesService.getProperty( PROPERTY_IDENTITY_SERVICE_CLIENT_CODE );

    private static Map<String, ValidationInfos> _mapValidationInfos = new ConcurrentHashMap<String, ValidationInfos>( );

    /**
     * Starts the validation process by generating and sending a validation code
     *
     * @param request
     *            The HTTP request
     * @throws fr.paris.lutece.portal.service.security.UserNotSignedException
     *             if no user found
     */
    public void startValidation( HttpServletRequest request ) throws UserNotSignedException
    {

        HttpSession session = request.getSession( true );
        ValidationInfos infos = new ValidationInfos( );
        infos.setExpiresTime( getExpiresTime( ) );
        infos.setUserConnectionId( getUserConnectionId( request ) );
        infos.setUserEmail( getUserEmail( request ) );

        _mapValidationInfos.put( session.getId( ), infos );
    }

    /**
     * Validate a validation code
     *
     * @param request
     *            The request
     * @param userInfo
     *            UserInfo from FranceConnect
     * @return A validation result
     */
    public ValidationResult validate( HttpServletRequest request, UserInfo userInfo )
    {
        HttpSession session = request.getSession( );

        if ( session == null )
        {
            return ValidationResult.SESSION_EXPIRED;
        }

        String strKey = session.getId( );
        ValidationInfos infos = _mapValidationInfos.get( strKey );

        if ( infos == null )
        {
            return ValidationResult.SESSION_EXPIRED;
        }

        _mapValidationInfos.remove( strKey );

        infos.setFCUserInfo( new FcIdentity( userInfo ) );

        certify( infos );

        return ValidationResult.OK;
    }

    /**
     * Certify attributes
     * @param infos Validation infos
     */
    public void certify( ValidationInfos infos )
    {
        IdentityService identityService = SpringContextService.getBean( BEAN_IDENTITYSTORE_SERVICE );

        IdentityChangeDto identityChange = new IdentityChangeDto( );
        IdentityDto identity = new IdentityDto( );

        identity.setConnectionId( infos.getUserConnectionId( ) );
        Map<String, AttributeDto> mapAttributes = new ConcurrentHashMap<>( );
        FcIdentity user = infos.getFCUserInfo( );
        addAttribute( mapAttributes, "birthdate", user.getIdsBirthDate( ) );
        addAttribute( mapAttributes, "birthplace", user.getIdsBirthPlace( ) );
        addAttribute( mapAttributes, "birthcountry", user.getIdsBirthCountry( ) );
        addAttribute( mapAttributes, "gender", user.getIdsGender( ) );
        addAttribute( mapAttributes, "first_name", user.getGivenName( ) );
        addAttribute( mapAttributes, "family_name", user.getFamilyName( ) );
        addAttribute( mapAttributes, "fc_gender", user.getGender( ) );
        addAttribute( mapAttributes, "fc_given_name", user.getGivenName( ) );
        addAttribute( mapAttributes, "fc_family_name", user.getFamilyName() );
        addAttribute( mapAttributes, "fc_birthdate", user.getBirthDate( ) );
        addAttribute( mapAttributes, "fc_birthplace", user.getBirthPlace( ) );
        addAttribute( mapAttributes, "fc_birthcountry", user.getBirthCountry( ) );

        identity.setAttributes( mapAttributes );
        identityChange.setIdentity( identity );

        AuthorDto author = new AuthorDto( );
        author.setApplicationCode( CLIENT_CODE );
        author.setType( AuthorType.TYPE_USER_OWNER.getTypeValue( ) );
        author.setId( AuthorDto.USER_DEFAULT_ID );
        identityChange.setAuthor( author );

        identityService.updateIdentity( identityChange, new HashMap<>( ) );
        //get Certifier Listener
        List<ICertifierListener> listCertifyListener=SpringContextService.getBeansOfType(ICertifierListener.class);
        if(listCertifyListener!=null)
        {
        	//Notify listener for new certify user
        	listCertifyListener.forEach(x->x.addCertifiedUser(infos));
        	
        }
        
    }

    /**
     * Add attribute DTO to a map
     * @param map The map
     * @param strKey The attribute key
     * @param strValue The attribute value
     */
    private void addAttribute( Map<String, AttributeDto> map, String strKey, String strValue )
    {
        AttributeDto attribute = new AttributeDto( );
        attribute.setKey( strKey );
        attribute.setValue( strValue );
        CertificateDto certificateDto = new CertificateDto( );
        certificateDto.setCertifierCode( CERTIFIER_CODE );
        attribute.setCertificate( certificateDto );
        map.put( attribute.getKey( ), attribute );
    }

    /**
     * Get the Identity DTO from a connection ID
     * @param strConnectionId The connection ID
     * @return The identity
     */
    public static IdentityDto getIdentity( String strConnectionId )
    {
        IdentityService identityService = SpringContextService.getBean( BEAN_IDENTITYSTORE_SERVICE );

        return identityService.getIdentityByConnectionId( strConnectionId, CLIENT_CODE );
    }

    /**
     * returns the user connection ID
     *
     * @param request
     *            The HTTP request
     * @return the user connection ID
     * @throws UserNotSignedException
     *             If no user is connected
     */
    private static String getUserConnectionId( HttpServletRequest request ) throws UserNotSignedException
    {
        if ( SecurityService.isAuthenticationEnable( ) )
        {
            LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );

            if ( user != null )
            {
                return user.getName( );
            }
            else
            {
                throw new UserNotSignedException( );
            }
        }
        else
        {
            return MOCKED_USER_CONNECTION_ID;
        }
    }

    /**
     * returns the user email
     *
     * @param request
     *            The HTTP request
     * @return the user connection ID
     * @throws UserNotSignedException
     *             If no user is connected
     */
    private static String getUserEmail( HttpServletRequest request ) throws UserNotSignedException
    {
        if ( SecurityService.isAuthenticationEnable( ) )
        {
            LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );

            if ( user != null )
            {
                return user.getEmail( );
            }
            else
            {
                throw new UserNotSignedException( );
            }
        }
        return MOCKED_USER_EMAIL;
    }

    /**
     * Calculate an expiration time
     *
     * @return the time as a long value
     */
    private static long getExpiresTime( )
    {
        return now( ) + ( (long) EXPIRES_DELAY * 60000L );
    }

    /**
     * The current time as a long value
     *
     * @return current time as a long value
     */
    private static long now( )
    {
        return ( new Date( ) ).getTime( );
    }

    /**
     * Enumeration of all validation results
     */
    public enum ValidationResult
    {
        OK( MESSAGE_CODE_VALIDATION_OK ), INVALID_CODE( MESSAGE_CODE_VALIDATION_INVALID ), SESSION_EXPIRED( MESSAGE_SESSION_EXPIRED ), CODE_EXPIRED(
                MESSAGE_CODE_EXPIRED ), TOO_MANY_ATTEMPS( MESSAGE_TOO_MANY_ATTEMPS );

        private String _strMessageKey;

        /**
         * Constructor
         *
         * @param strMessageKey
         *            The i18n message key
         */
        ValidationResult( String strMessageKey )
        {
            _strMessageKey = strMessageKey;
        }

        /**
         * Return the i18n message key
         *
         * @return the i18n message key
         */
        public String getMessageKey( )
        {
            return _strMessageKey;
        }

    }
    
    /**
     * delete FranceConnect certification
     * @param request
     * @throws UserNotSignedException 
     */
    public void deleteCertification ( HttpServletRequest request ) throws UserNotSignedException
    {
        IdentityService identityService = SpringContextService.getBean( BEAN_IDENTITYSTORE_SERVICE );
        
        String strConnectionId = getUserConnectionId( request );
        
        AuthorDto author = new AuthorDto( );
        IdentityChangeDto identityChange = new IdentityChangeDto( );
        IdentityDto identity = getIdentity( strConnectionId );
        
        deleteCertificateAttribute ( identity.getAttributes( ).get( "birthdate" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "birthplace" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "birthcountry" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "gender" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "first_name" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "family_name" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_gender" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_given_name" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_family_name" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_birthdate" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_birthplace" ) );
        deleteCertificateAttribute ( identity.getAttributes( ).get( "fc_birthcountry" ) );

        identityChange.setIdentity( identity );
        author.setApplicationCode( CLIENT_CODE );
        author.setType( AuthorType.TYPE_USER_OWNER.getTypeValue( ) );
        author.setId( AuthorDto.USER_DEFAULT_ID );
        
        identityChange.setAuthor( author );
        
        identityService.updateIdentity( identityChange, null );
        
        List<ICertifierListener> listCertifyListener=SpringContextService.getBeansOfType(ICertifierListener.class);
        if(listCertifyListener!=null)
        {
        	//Notify listener for new certify user
        	listCertifyListener.forEach(x->x.removeCertifiedUser(strConnectionId));
        	
        }
        
        
        
    }
    
    /**
     * Drop the certificate of an attribute
     * @param attribute the attribute to drop the certificate
     */
    private void deleteCertificateAttribute( AttributeDto attribute )
    {
        if ( attribute != null  
                && attribute.getCertificate( ) !=null 
                && attribute.getCertificate( ).getCertifierCode( ).equals( CERTIFIER_CODE ) )
        {
            attribute.setCertificate( null );
            attribute.setValue( StringUtils.EMPTY );
            attribute.setCertified(false);
        }
    }
}
