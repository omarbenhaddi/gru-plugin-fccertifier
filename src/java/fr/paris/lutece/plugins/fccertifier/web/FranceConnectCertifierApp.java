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
package fr.paris.lutece.plugins.fccertifier.web;

import fr.paris.lutece.plugins.fccertifier.dataclient.UserDataClient;
import fr.paris.lutece.plugins.fccertifier.business.FcIdentity;
import fr.paris.lutece.plugins.fccertifier.service.CertifierService;
import fr.paris.lutece.plugins.fccertifier.service.CertifierService.ValidationResult;
import fr.paris.lutece.plugins.franceconnect.oidc.UserInfo;
import fr.paris.lutece.plugins.franceconnect.service.DataClientService;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.IdentityDto;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import fr.paris.lutece.portal.web.xpages.XPage;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * FranceConnect Certifier App
 */
@Controller( xpageName = "fccertifier", pageTitleI18nKey = "fccertifier.xpage.fccertifier.pageTitle", pagePathI18nKey = "fccertifier.xpage.fccertifier.pagePathLabel" )
public class FranceConnectCertifierApp extends MVCApplication
{
    public static final String VIEW_HOME = "home";
    public static final String VIEW_VALIDATION = "validation";
    public static final String VIEW_VALIDATION_OK = "validationOK";
    public static final String VIEW_VALIDATE_FC_DATA = "validate";

    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE_HOME = "skin/plugins/identitystore/modules/fccertifier/home.html";
    private static final String TEMPLATE_VALIDATION_OK = "skin/plugins/identitystore/modules/fccertifier/validation_ok.html";
    private static final String TEMPLATE_VALIDATE_DATA = "skin/plugins/identitystore/modules/fccertifier/validate_data.html";
    private static final String ACTION_FETCH_FC_DATA = "fetch";
    private static final String ACTION_CERTIFY = "certify";
    private static final String DATACLIENT_USER = "user";
    private static final String MARK_FC_INFOS = "fc_infos";
    private static final String MARK_IDENTITY = "identity";

    private CertifierService _certifierService;

    /**
     * Constructor for init
     */
    public FranceConnectCertifierApp( )
    {
        super( );
        _certifierService = new CertifierService( );
    }

    /**
     * Gets the Home page
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     * @throws UserNotSignedException
     *             if user is not connected
     */
    @View( value = VIEW_HOME, defaultView = true )
    public XPage home( HttpServletRequest request ) throws UserNotSignedException
    {
        checkUserAuthentication( request );

        _certifierService.startValidation( request );

        return getXPage( TEMPLATE_HOME, LocaleService.getDefault( ), getModel( ) );
    }

    /**
     * Fetch data from FrancConnect
     *
     * @param request
     *            The HTTP request
     * @return The redirected page
     * @throws UserNotSignedException
     *             if no user is connected
     */
    @Action( ACTION_FETCH_FC_DATA )
    public XPage doFetch( HttpServletRequest request ) throws UserNotSignedException
    {
        checkUserAuthentication( request );

        String strUrl = DataClientService.instance( ).getDataClientUrl( DATACLIENT_USER );
        return redirect( request, strUrl );
    }

    /**
     * Validate FranceConnect data
     * 
     * @param request
     *            The HTTP request
     * @return The page
     * @throws UserNotSignedException
     *             if user not signed
     */
    @View( VIEW_VALIDATE_FC_DATA )
    public XPage validationFCData( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = checkUserAuthentication( request );
        Map<String, Object> model = getModel( );

        UserInfo fcUserInfo = (UserInfo) request.getSession( ).getAttribute( UserDataClient.ATTRIBUTE_USERINFO );
        IdentityDto identity = CertifierService.getIdentity( user.getName( ) );

        model.put( MARK_FC_INFOS, new FcIdentity( fcUserInfo ) );
        model.put( MARK_IDENTITY, identity );

        return getXPage( TEMPLATE_VALIDATE_DATA, LocaleService.getDefault( ), model );

    }

    @Action( ACTION_CERTIFY )
    public XPage doCertify( HttpServletRequest request ) throws UserNotSignedException
    {
        checkUserAuthentication( request );
        UserInfo fcUserInfo = (UserInfo) request.getSession( ).getAttribute( UserDataClient.ATTRIBUTE_USERINFO );

        ValidationResult result = _certifierService.validate( request, fcUserInfo );

        if ( result != ValidationResult.OK )
        {
            addError( result.getMessageKey( ), LocaleService.getDefault( ) );

            if ( result == ValidationResult.SESSION_EXPIRED )
            {
                return redirectView( request, VIEW_HOME );
            }
        }
        return redirectView( request, VIEW_VALIDATION_OK );
    }

    /**
     * Displays Validation OK page
     *
     * @param request
     *            The HTTP request
     * @return The page
     * @throws UserNotSignedException
     *             if user is not connected
     */
    @View( VIEW_VALIDATION_OK )
    public XPage validationOK( HttpServletRequest request ) throws UserNotSignedException
    {
        checkUserAuthentication( request );

        return getXPage( TEMPLATE_VALIDATION_OK );
    }

    /**
     * check if user is authenticated
     *
     * @param request
     *            request
     * @throws UserNotSignedException
     *             if user is not connected
     */
    private LuteceUser checkUserAuthentication( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser luteceUser = SecurityService.isAuthenticationEnable( ) ? SecurityService.getInstance( ).getRegisteredUser( request ) : null;

        if ( luteceUser == null )
        {
            throw new UserNotSignedException( );
        }
        return luteceUser;
    }

}
