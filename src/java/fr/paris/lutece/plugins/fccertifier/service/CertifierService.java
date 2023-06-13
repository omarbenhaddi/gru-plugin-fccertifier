/*
 * Copyright (c) 2002-2023, Mairie de Paris
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;

import fr.paris.lutece.plugins.fccertifier.business.FcIdentity;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.CertifiedAttribute;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.Identity;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeRequest;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.IdentitySearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.QualifiedIdentity;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.oauth2.modules.franceconnect.business.UserInfo;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
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
    private static final String CERTIFIER_CODE = "FC";
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

        IdentityChangeRequest identityChangeRequest = new IdentityChangeRequest( );

        Identity identity = new Identity( );
        identity.setConnectionId( infos.getUserConnectionId( ) );

        List<CertifiedAttribute> listCertifiedAttribute = new ArrayList<>();
                
        FcIdentity user = infos.getFCUserInfo( );
        
        Date date = new Date( );
        
        CertifiedAttribute certifiedAttribute = new CertifiedAttribute( );        
        certifiedAttribute.setKey( "birthdate" );
        certifiedAttribute.setValue( user.getIdsBirthDate( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );
        
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "birthplace" );
        certifiedAttribute.setValue( user.getIdsBirthPlace( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );       

        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "birthcountry" );
        certifiedAttribute.setValue( user.getIdsBirthCountry( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );      
        
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "gender" );
        certifiedAttribute.setValue( user.getIdsGender( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );     
        
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "first_name" );
        certifiedAttribute.setValue( user.getGivenName( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );   
  
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "family_name" );
        certifiedAttribute.setValue( user.getFamilyName( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );  
  
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_gender" );
        certifiedAttribute.setValue( user.getGender( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );  
  
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_given_name" );
        certifiedAttribute.setValue( user.getGivenName( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );  
 
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_family_name" );
        certifiedAttribute.setValue( user.getFamilyName( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );  
  
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_birthdate" );
        certifiedAttribute.setValue( user.getBirthDate( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );
  
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_birthplace" );
        certifiedAttribute.setValue( user.getBirthPlace( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );
 
        certifiedAttribute = new CertifiedAttribute( );
        certifiedAttribute.setKey( "fc_birthcountry" );
        certifiedAttribute.setValue( user.getBirthPlace( ) );
        certifiedAttribute.setCertificationProcess( CERTIFIER_CODE );
        certifiedAttribute.setCertificationDate( date );
        listCertifiedAttribute.add( certifiedAttribute );
        
        identity.setAttributes( listCertifiedAttribute );

        
        RequestAuthor author = new RequestAuthor( );
        author.setName( CLIENT_CODE );
        author.setType( AuthorType.application );
       
        identityChangeRequest.setIdentity( identity );
        identityChangeRequest.setOrigin( author );
        
        try
        {   
            identityService.updateIdentity( infos.getUserConnectionId( ), identityChangeRequest, CLIENT_CODE );
        } catch ( AppException | IdentityStoreException e )
        {
            AppLogService.error( "Error updating identity for {}", e.getMessage( ), infos.getUserConnectionId( ) );
        }
        //get Certifier Listener
        List<ICertifierListener> listCertifyListener=SpringContextService.getBeansOfType(ICertifierListener.class);
        if(listCertifyListener!=null)
        {
        	//Notify listener for new certify user
        	listCertifyListener.forEach(x->x.addCertifiedUser(infos));
        	
        }
        
    }


    /**
     * Get the Identity DTO from a connection ID
     * @param strConnectionId The connection ID
     * @return The identity
     */
    public static QualifiedIdentity getIdentity( String strConnectionId )
    {
        IdentityService identityService = SpringContextService.getBean( BEAN_IDENTITYSTORE_SERVICE );

        IdentitySearchResponse identitySearchResponse;
        try
        {
            identitySearchResponse = identityService.getIdentityByConnectionId( strConnectionId, CLIENT_CODE );
            
            if(  identitySearchResponse != null && !CollectionUtils.isEmpty( identitySearchResponse.getIdentities( ) ) )
            {
               return identitySearchResponse.getIdentities( ).get( 0 );
            }
        } catch ( AppException | IdentityStoreException e )
        {
            AppLogService.error( "Error to get identity by connection id {}", e.getMessage( ), strConnectionId );
        }
        
        return null;
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
  
}
