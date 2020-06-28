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
package fr.paris.lutece.plugins.fccertifier.dataclient;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.paris.lutece.plugins.fccertifier.web.FranceConnectCertifierApp;
import fr.paris.lutece.plugins.oauth2.business.Token;
import fr.paris.lutece.plugins.oauth2.dataclient.AbstractDataClient;
import fr.paris.lutece.plugins.oauth2.modules.franceconnect.business.UserInfo;
import fr.paris.lutece.plugins.oauth2.service.MapperService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.util.mvc.utils.MVCUtils;
import fr.paris.lutece.util.url.UrlItem;

/**
 * UserDataClient
 */
public class UserDataClient extends AbstractDataClient
{
    public static final String ATTRIBUTE_USERINFO = "fccertifier-userinfo";
    private static final String XPAGE_NAME = "fccertifier";

    /**
     * {@inheritDoc }
     */
    @Override
    public void handleToken( Token token, HttpServletRequest request, HttpServletResponse response )
    {
        try
        {
            UserInfo userInfo = MapperService.parse( getData( token ), UserInfo.class );
            request.getSession( true ).setAttribute( ATTRIBUTE_USERINFO, userInfo );

            String strRedirectUrl = getViewUrl( request, FranceConnectCertifierApp.VIEW_VALIDATE_FC_DATA );
            response.sendRedirect( strRedirectUrl );
        }
        catch( IOException ex )
        {
            AppLogService.error( "Error DataClient User : " + ex.getMessage( ), ex );
        }
    }

    /**
     * Build the view URL
     * @param request The HTTP request
     * @param strView The view name
     * @return The URL
     */
    public static String getViewUrl( HttpServletRequest request, String strView )
    {
        UrlItem url = new UrlItem( AppPathService.getBaseUrl( request ) + AppPathService.getPortalUrl( ) );
        url.addParameter( MVCUtils.PARAMETER_PAGE, XPAGE_NAME );
        url.addParameter( MVCUtils.PARAMETER_VIEW, strView );

        return url.toString( );
    }

    /**
     * Build the action URL
     * @param request The HTTP request
     * @param strAction The action name
     * @return The action URL
     */
    public static String getActionUrl( HttpServletRequest request, String strAction )
    {
        UrlItem url = new UrlItem( AppPathService.getBaseUrl( request ) + AppPathService.getPortalUrl( ) );
        url.addParameter( MVCUtils.PARAMETER_PAGE, XPAGE_NAME );
        url.addParameter( MVCUtils.PARAMETER_ACTION, strAction );

        return url.toString( );
    }
}
