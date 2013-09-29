/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.taxi.run;

import java.io.IOException;


/*package*/class KaiSingleIterTaxiLauncher
{
    public static void main(String... args)
        throws IOException
    {
        String file = "./shared-svn/projects/maciejewski/input/mielec-2-peaks/params.in";
        SingleIterTaxiLauncher launcher = new SingleIterTaxiLauncher(file);
        launcher.go(false);
        launcher.generateOutput();
    }
}
