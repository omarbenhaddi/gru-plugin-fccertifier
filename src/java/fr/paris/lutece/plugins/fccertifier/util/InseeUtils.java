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
package fr.paris.lutece.plugins.fccertifier.util;


import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.geocode.v1.web.rs.dto.City;
import fr.paris.lutece.plugins.geocode.v1.web.rs.dto.Country;
import fr.paris.lutece.plugins.geocode.v1.web.service.GeoCodeService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.date.DateUtil;


/**
 * InseeUtils
 */
public final class InseeUtils
{
     private static GeoCodeService _geoCodeService = SpringContextService.getBean( "geocodes.geoCodesService" );
     
    /**
     * Private constructor
     */
    private InseeUtils()
    {
    }
    
    /**
     * Give the name of the place for a given INSEE code
     * 
     * @param strCode
     *            The INSEE code
     * @param strBirthdate
     *            The birthdate
     * @return The name
     */
    public static String getPlaceName( String strCode, String strBirthdate )
    {
        Date birthdate = DateUtil.formatDate( strBirthdate, Locale.FRANCE );

        try
        {
            City city = _geoCodeService.getCityByCodeAndDate( strCode, birthdate );
            if ( city != null )
            {
                return city.getValue( );
            }
        } catch ( Exception e )
        {
            AppLogService.error( "Une erreur s'est produite lors de la récupération de la ville de naissance pour le code INSEE {}", strCode, e.getMessage( ) );
        }

        return StringUtils.EMPTY;
    }

    /**
     * Give the name of the country for a given INSEE code
     * 
     * @param strCode
     *            The INSEE code
     * @param strBirthdate
     *            The birthdate
     * @return The name
     */
    public static String getCountryName( String strCode, String strBirthdate )
    {
        Date birthdate = DateUtil.formatDate( strBirthdate, Locale.FRANCE );

        try
        {
            Country country = _geoCodeService.getCountryByCodeAndDate( strCode, birthdate );
            if ( country != null )
            {
                return country.getValue( );
            }
        } catch ( Exception e )
        {
            AppLogService.error( "Une erreur s'est produite lors de la récupération du pays de naissance pour le code INSEE {}", strCode, e.getMessage( ) );
        }

        return StringUtils.EMPTY;
    }
}
