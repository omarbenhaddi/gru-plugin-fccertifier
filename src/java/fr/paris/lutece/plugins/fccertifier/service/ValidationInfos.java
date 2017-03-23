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

import fr.paris.lutece.plugins.fccertifier.business.FcIdentity;

/**
 * ValidationInfos
 */
public class ValidationInfos
{
    // Variables declarations
    private String _strUserEmail;
    private String _strUserConnectionId;
    private FcIdentity _identity;
    private long _expiresTime;
    private int _nInvalidAttempts;

    /**
     * Returns the FC FcIdentity
     *
     * @return The FC FcIdentity
     */
    public FcIdentity getFCUserInfo( )
    {
        return _identity;
    }

    /**
     * Sets the FCUserInfo
     *
     * @param userInfo
     *            The FC FcIdentity
     */
    public void setFCUserInfo( FcIdentity userInfo )
    {
        _identity = userInfo;
    }

    /**
     * Returns the UserConnectionId
     *
     * @return The UserConnectionId
     */
    public String getUserConnectionId( )
    {
        return _strUserConnectionId;
    }

    /**
     * @return the _strUserEmail
     */
    public String getUserEmail( )
    {
        return _strUserEmail;
    }

    /**
     * @param strUserEmail
     *            the _strUserEmail to set
     */
    public void setUserEmail( String strUserEmail )
    {
        _strUserEmail = strUserEmail;
    }

    /**
     * Sets the UserConnectionId
     *
     * @param strUserConnectionId
     *            The UserConnectionId
     */
    public void setUserConnectionId( String strUserConnectionId )
    {
        _strUserConnectionId = strUserConnectionId;
    }

    /**
     * Returns the expiresTime
     *
     * @return The expiresTime
     */
    public long getExpiresTime( )
    {
        return _expiresTime;
    }

    /**
     * Sets the expiresTime
     *
     * @param expiresTime
     *            The expiresTime
     */
    public void setExpiresTime( long expiresTime )
    {
        _expiresTime = expiresTime;
    }

    /**
     * Returns the InvalidAttempts
     *
     * @return The InvalidAttempts
     */
    public int getInvalidAttempts( )
    {
        return _nInvalidAttempts;
    }

    /**
     * Sets the InvalidAttempts
     *
     * @param nInvalidAttempts
     *            The InvalidAttempts
     */
    public void setInvalidAttempts( int nInvalidAttempts )
    {
        _nInvalidAttempts = nInvalidAttempts;
    }
}
