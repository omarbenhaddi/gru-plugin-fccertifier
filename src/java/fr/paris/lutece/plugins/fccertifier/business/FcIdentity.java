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

package fr.paris.lutece.plugins.fccertifier.business;


import fr.paris.lutece.plugins.fccertifier.util.DateUtils;
import fr.paris.lutece.plugins.fccertifier.util.InseeUtils;
import fr.paris.lutece.plugins.oauth2.modules.franceconnect.business.UserInfo;

/**
 * FcIdentity
 */
public class FcIdentity extends UserInfo
{
    private String _strIdsBirthDate;
    private String _strIdsBirthPlace;
    private String _strIdsBirthCountry;

    /**
     * Constructor
     * @param userInfo UserInfo
     */
    public FcIdentity( UserInfo userInfo )
    {
        super();
        
        setBirthDate( userInfo.getBirthDate( ) );
        setBirthPlace( userInfo.getBirthPlace( ) );
        setBirthCountry( userInfo.getBirthCountry( ) );
        setEmail( userInfo.getEmail( ) );
        setEmailVerified( userInfo.getEmailVerified( ) );
        setFamilyName( userInfo.getFamilyName( ) );
        setGender( userInfo.getGender( ) );
        setGivenName( userInfo.getGivenName( ) );
        setMiddleName( userInfo.getMiddleName( ) );
        setName( userInfo.getName( ) );
        setNickname( userInfo.getNickname( ) );
        setPhoneNumber( userInfo.getPhoneNumber( ) );
        setPhoneNumberVerified( userInfo.getPhoneNumberVerified( ) );
        setPreferredUsername( userInfo.getPreferredUsername( ) );

        _strIdsBirthDate = DateUtils.convertFcToIds( getBirthDate( ) );
        _strIdsBirthPlace = InseeUtils.getPlaceName( getBirthPlace( ) );
        _strIdsBirthCountry = InseeUtils.getCountryName( getBirthCountry( ) );

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

    /**
     * Gender conversion
     * @return The Gender in IDStore format
     */
    public String getIdsGender( )
    {
        if ( getGender( ).equals( "male" ) )
        {
            return "2";
        }
        else
            if ( getGender( ).equals( "female" ) )
            {
                return "1";
            }
        return "0";
    }
}
