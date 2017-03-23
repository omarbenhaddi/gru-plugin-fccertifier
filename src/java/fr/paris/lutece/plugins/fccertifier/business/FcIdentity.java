package fr.paris.lutece.plugins.fccertifier.business;

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

import fr.paris.lutece.plugins.fccertifier.util.DateUtils;
import fr.paris.lutece.plugins.fccertifier.util.InseeUtils;
import fr.paris.lutece.plugins.franceconnect.oidc.UserInfo;

/**
 * FcIdentity
 */
public class FcIdentity extends UserInfo
{
    private String _strIdsBirthDate;
    private String _strIdsBirthPlace;
    private String _strIdsBirthCountry;

    public FcIdentity( UserInfo ui )
    {
        setBirthDate( ui.getBirthDate( ) );
        setBirthPlace( ui.getBirthPlace( ) );
        setBirthCountry( ui.getBirthCountry( ) );
        setEmail( ui.getEmail( ) );
        setEmailVerified( ui.getEmailVerified( ) );
        setFamilyName( ui.getFamilyName( ) );
        setGender( ui.getGender( ) );
        setGivenName( ui.getGivenName( ) );
        setMiddleName( ui.getMiddleName( ) );
        setName( ui.getName( ) );
        setNickname( ui.getNickname( ) );
        setPhoneNumber( ui.getPhoneNumber( ) );
        setPhoneNumberVerified( ui.getPhoneNumberVerified( ) );
        setPreferredUsername( ui.getPreferredUsername( ) );

        // FIXME
        if ( getBirthPlace( ) == null )
        {
            setBirthPlace( "67482" );
        }

        _strIdsBirthDate = DateUtils.convertFcToIds( getBirthDate( ) );
        _strIdsBirthPlace = InseeUtils.getPlaceName( getBirthPlace( ) );
        if ( _strIdsBirthDate != null && !_strIdsBirthDate.equals( "" ) )
        {
            _strIdsBirthCountry = "FRANCE";
        }
        else
        {
            _strIdsBirthCountry = InseeUtils.getCountryName( getBirthCountry( ) );
        }

    }

    /**
     * Returns the IdsBirthDate
     * 
     * @return The IdsBirthDate
     */
    public String getIdsBirthDate( )
    {
        return _strIdsBirthDate;
    }

    /**
     * Sets the IdsBirthDate
     * 
     * @param strIdsBirthDate
     *            The IdsBirthDate
     */
    public void setIdsBirthDate( String strIdsBirthDate )
    {
        _strIdsBirthDate = strIdsBirthDate;
    }

    /**
     * Returns the IdsBirthPlace
     * 
     * @return The IdsBirthPlace
     */
    public String getIdsBirthPlace( )
    {
        return _strIdsBirthPlace;
    }

    /**
     * Sets the IdsBirthPlace
     * 
     * @param strIdsBirthPlace
     *            The IdsBirthPlace
     */
    public void setIdsBirthPlace( String strIdsBirthPlace )
    {
        _strIdsBirthPlace = strIdsBirthPlace;
    }

    /**
     * Returns the IdsBirthCountry
     * 
     * @return The IdsBirthCountry
     */
    public String getIdsBirthCountry( )
    {
        return _strIdsBirthCountry;
    }

    /**
     * Sets the IdsBirthCountry
     * 
     * @param strIdsBirthCountry
     *            The IdsBirthCountry
     */
    public void setIdsBirthCountry( String strIdsBirthCountry )
    {
        _strIdsBirthCountry = strIdsBirthCountry;
    }

    // FIXME
    public String getIdsGender( )
    {
        if ( getGender( ).equals( "male" ) )
        {
            return "1";
        }
        else
            if ( getGender( ).equals( "female" ) )
            {
                return "2";
            }
        return "0";
    }
}
