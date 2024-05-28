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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.fccertifier.business.FcIdentity;
import fr.paris.lutece.plugins.identityquality.v3.web.service.IdentityQualityService;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AttributeDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.ResponseStatusType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeRequest;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.DuplicateSearchRequest;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.DuplicateSearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.IdentitySearchResponse;
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
    private static final String CERTIFIER_CODE = "fccertifier";
    private static final String CERTIFIER_CODE_DEFAULT = "DEC";
    private static final String BEAN_IDENTITY_QUALITY_SERVICE = "fccertifier.identityQualityService";
    private static final boolean PROPERTY_SUSPICIOUS_IDENTITY_ACTIVATION_INDICATEUR = AppPropertiesService.getPropertyBoolean( "mydashboard.identity.suspicious.activation_indicator", false );
    private static final String PROPERTY_SUSPICIOUS_LIST_RULE_STRIC = AppPropertiesService.getProperty( "mydashboard.identity.suspicious.identity.list_code_rule.strict", "RG_GEN_StrictDoublon_01" );
    private static final String PROPERTY_SUSPICIOUS_LIST_RULE_NOT_STRIC = AppPropertiesService.getProperty( "mydashboard.identity.suspicious.identity.list_code_rule.not_strict",
            "RG_GEN_SuspectDoublon_09" );   
    private static final String CLIENT_CODE = AppPropertiesService.getProperty( PROPERTY_IDENTITY_SERVICE_CLIENT_CODE );

    private static Map<String, ValidationInfos> _mapValidationInfos = new ConcurrentHashMap<String, ValidationInfos>( );
    private static ObjectMapper _mapper;
    static
    {
        _mapper = new ObjectMapper( );
        _mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
    }

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

        IdentityDto identityStore  =getIdentity(infos.getUserConnectionId());
        
        if(identityStore!=null)
            
        {
        
        
            IdentityChangeRequest identityChangeRequest = new IdentityChangeRequest( );
    
            IdentityDto identity = new IdentityDto( );
            identity.setCustomerId(identityStore.getConnectionId());
            identity.setLastUpdateDate(identityStore.getLastUpdateDate());
    
            List<AttributeDto> listCertifiedAttribute = new ArrayList<>();
                    
            FcIdentity user = infos.getFCUserInfo( );
            
            Date date = new Date( );
            
          
            addCertificateAttribute("birthdate",user.getIdsBirthDate( ),date,listCertifiedAttribute );
            addCertificateAttribute("birthplace_code", user.getBirthPlace( ),date ,listCertifiedAttribute );
            addCertificateAttribute("birthcountry_code",  user.getBirthCountry(),date,listCertifiedAttribute );
            addCertificateAttribute("gender",  user.getIdsGender( ),date ,listCertifiedAttribute );
            addCertificateAttribute("first_name", user.getGivenName( ),date ,listCertifiedAttribute );
            addCertificateAttribute("family_name", user.getFamilyName( ) ,date ,listCertifiedAttribute );
            addCertificateAttribute("preferred_username", user.getPreferredUsername() ,date ,listCertifiedAttribute );
          
            identity.setAttributes( listCertifiedAttribute );
    
            
            RequestAuthor author = new RequestAuthor( );
            author.setName( CLIENT_CODE );
            author.setType( AuthorType.application );
           
            identityChangeRequest.setIdentity( identity );
           
            
            try
            {   
                
                final IdentityChangeResponse response= identityService.updateIdentity( identityStore.getCustomerId(), identityChangeRequest, CLIENT_CODE,author);
                if (response == null || !ResponseStatusType.OK.equals(response.getStatus().getType())  )
                  {
                      AppLogService.error( "Error when  updating the identity for connectionId {} the idantity change status is {}, the identity response is {} ", identity.getConnectionId( ), response!=null? response.getStatus():"",printJsonObjectAsString(response));
                      
                      throw new IdentityStoreException(response!=null ? response.getStatus().getType().name():"");
                  }
                
            } catch ( AppException | IdentityStoreException e )
            {
                AppLogService.error( "Error updating identity for {}", infos.getUserConnectionId( ) ,e.getMessage( ));
            }
            //get Certifier Listener
            List<ICertifierListener> listCertifyListener=SpringContextService.getBeansOfType(ICertifierListener.class);
            if(listCertifyListener!=null)
            {
                //Notify listener for new certify user
                listCertifyListener.forEach(x->x.addCertifiedUser(infos));
                
            }
           }
        else
        {
            AppLogService.error( "error when upadating identity information for connetion Id {}, no Identity found", infos.getUserConnectionId( ) );
            
        }
        
    }


    /**
     * Get the Identity DTO from a connection ID
     * @param strConnectionId The connection ID
     * @return The identity
     */
    public static IdentityDto getIdentity( String strConnectionId )
    {
        IdentityService identityService = SpringContextService.getBean( BEAN_IDENTITYSTORE_SERVICE );

        IdentitySearchResponse identitySearchResponse;
        RequestAuthor  requestAuthor = new RequestAuthor(  );
         requestAuthor.setName( CLIENT_CODE );
        requestAuthor.setType( AuthorType.owner );
        
        try
        {
            identitySearchResponse = identityService.getIdentityByConnectionId( strConnectionId, CLIENT_CODE , requestAuthor );
                        
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
    
    
    private  void  addCertificateAttribute( String strKey,String strValue,Date certDate,List<AttributeDto> listCertifiedAttribute)
    {
       
        
        addCertificateAttribute(strKey, strValue, certDate, false,listCertifiedAttribute);
        
    }
    
    
    private  void addCertificateAttribute( String strKey,String strValue,Date certDate,boolean bDefault,List<AttributeDto> listCertifiedAttribute)
    {
          
        if(!StringUtils.isEmpty(strValue))
        {
            AttributeDto certifiedAttribute = new AttributeDto( );        
            certifiedAttribute.setKey( strKey );
            certifiedAttribute.setValue( strValue!=null? strValue:"");
            certifiedAttribute.setCertifier( bDefault?CERTIFIER_CODE_DEFAULT:CERTIFIER_CODE );
            certifiedAttribute.setCertificationDate( certDate );
            
            listCertifiedAttribute.add(certifiedAttribute);
        }
     
     
            
    }
    
    /**
     * Get suspicious identities API
     * @param userInfo
     * @param listRules
     * @return DuplicateSearchResponse
     */
    public static DuplicateSearchResponse getSuspiciousIdentitiesAPI( FcIdentity fcIdentity, List<String> listRules )
    {     
        if( PROPERTY_SUSPICIOUS_IDENTITY_ACTIVATION_INDICATEUR )
        {
            IdentityQualityService identityQualityService = SpringContextService.getBean( BEAN_IDENTITY_QUALITY_SERVICE );
            
            RequestAuthor author = new RequestAuthor( );
            author.setName( CLIENT_CODE );
            author.setType( AuthorType.application );
            
            DuplicateSearchRequest duplicateSearchRequest = new DuplicateSearchRequest( );
            duplicateSearchRequest.setRuleCodes( listRules );
                    
            initAttributeSuspiciousSearchRequest( duplicateSearchRequest, fcIdentity );
                   
            try
            {
                return identityQualityService.searchDuplicates( duplicateSearchRequest, CLIENT_CODE, author );           
            }
            catch ( IdentityStoreException | AppException ex )
            {
                AppLogService.info( "Error getting Search duplicate identities ", ex );
            }
        }
        return null;
    }
    
    /**
     * Init attribute for suspicious search request
     * @param duplicateSearchRequest
     * @param fcIdentity
     */
    private static void initAttributeSuspiciousSearchRequest ( DuplicateSearchRequest duplicateSearchRequest, FcIdentity fcIdentity )
    {
        Map<String, String> mapAttributes = new HashMap<>( );
        
        if( StringUtils.isNotEmpty( fcIdentity.getPreferredUsername( ) ) )
        {
            mapAttributes.put( "preferred_username", fcIdentity.getPreferredUsername( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getIdsGender( ) ) )
        {
            mapAttributes.put( "gender", fcIdentity.getIdsGender( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getIdsBirthDate( ) ) )
        {
            mapAttributes.put( "birthdate", fcIdentity.getIdsBirthDate( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getBirthCountry( ) ) )
        {
            mapAttributes.put( "birthcountry_code", fcIdentity.getBirthCountry( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getBirthPlace( ) ) )
        {
            mapAttributes.put( "birthplace_code", fcIdentity.getBirthPlace( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getFamilyName( ) ) )
        {
            mapAttributes.put( "family_name", fcIdentity.getFamilyName( ) );
        }
        
        if( StringUtils.isNotEmpty( fcIdentity.getGivenName( ) ) )
        {
            mapAttributes.put( "first_name", fcIdentity.getGivenName( ) );
        }
       
        duplicateSearchRequest.setAttributes( mapAttributes );
    }
    
    /**
     * Return true if exist an strict suscpicious identities
     * @param fcIdentity
     * @param strConnectionId
     * @return
     */
    public static boolean existStrictSuspiciousIdentities( FcIdentity fcIdentity, String strConnectionId )
    {
        DuplicateSearchResponse suspiciousSearchResponse = getSuspiciousIdentitiesAPI( fcIdentity, Arrays.asList( PROPERTY_SUSPICIOUS_LIST_RULE_NOT_STRIC.split( ";" ) ) ) ;
        
        if( suspiciousSearchResponse != null && suspiciousSearchResponse.getStatus( ) != null && suspiciousSearchResponse.getStatus( ).getType( ).equals( ResponseStatusType.OK ) &&
                CollectionUtils.isNotEmpty( suspiciousSearchResponse.getIdentities( ) ) && StringUtils.isNotEmpty( strConnectionId ) )
        {
            for( IdentityDto identity : suspiciousSearchResponse.getIdentities( ) )
            {
                if( StringUtils.isEmpty( identity.getConnectionId( ) ) 
                        || ( StringUtils.isNotEmpty( identity.getConnectionId( ) ) && !identity.getConnectionId( ).equalsIgnoreCase( strConnectionId ) ) )
                {
                    return true;
                }
            }              
        }
        return false;
    }
    
    /**
     * Print JsonObject as String
     * @param o the json Object
     * @return Json String
     */
    public static String printJsonObjectAsString(Object o) 
    {
		if (o != null) {

			try {
				return _mapper.writeValueAsString(o);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				AppLogService.error("Failed to write object as Json", e);
			}
		}
    	return "";
    }
    
}