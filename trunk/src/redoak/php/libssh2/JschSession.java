/*
 * 
 *	Copyright (C) 2011 -- Alfred Reibenschuh
 *
 * 	This program is free software; you can redistribute it and/or 
 *	modify it under the terms of the GNU General Public License as 
 *	published by the Free Software Foundation; either version 2 of 
 *	the License, or (at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, 
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 *	GNU General Public License for more details.
 * 
 */

package redoak.php.libssh2;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JschSession
{
	public JSch jsch;
	public Session session;
	public String host;
	public int port;

}
