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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.file.AbstractBinaryInputOutput;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.BufferedBinaryInputOutput;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

public class Libssh2Module extends AbstractQuercusModule
{
	private static final Logger log
	= Logger.getLogger(Libssh2Module.class.getName());
	private static final L10N L = new L10N(Libssh2Module.class);

//  Flag to ssh2_fingerprint() requesting hostkey fingerprint as an MD5 hash. 
	public static final int SSH2_FINGERPRINT_MD5         = 0x0000;
//    Flag to ssh2_fingerprint() requesting hostkey fingerprint as an SHA1 hash. 
	public static final int SSH2_FINGERPRINT_SHA1        = 0x0001;
//    Flag to ssh2_fingerprint() requesting hostkey fingerprint as a string of hexits. 
	public static final int SSH2_FINGERPRINT_HEX         = 0x0000;
//    Flag to ssh2_fingerprint() requesting hostkey fingerprint as a raw string of 8-bit characters. 
	public static final int SSH2_FINGERPRINT_RAW         = 0x0002;

//    Flag to ssh2_shell() specifying that width and height are provided as character sizes. 
	public static final int SSH2_TERM_UNIT_CHARS         = 0x0000;
//  Flag to ssh2_shell() specifying that width and height are provided in pixel units. 
	public static final int SSH2_TERM_UNIT_PIXELS        = 0x0001;

//    Default terminal type (e.g. vt102, ansi, xterm, vanilla) requested by ssh2_shell().
	public static final String SSH2_DEFAULT_TERMINAL     = "vanilla";
//    Default terminal width requested by ssh2_shell(). 
	public static final int SSH2_DEFAULT_TERM_WIDTH      = 80;
//    Default terminal height requested by ssh2_shell(). 
	public static final int SSH2_DEFAULT_TERM_HEIGHT     = 25;
//    Default terminal units requested by ssh2_shell(). 
	public static final int SSH2_DEFAULT_TERM_UNIT       = SSH2_TERM_UNIT_CHARS;

// SSH2_STREAM_STDIO (integer)
//  Flag to ssh2_fetch_stream() requesting STDIO subchannel. 
//SSH2_STREAM_STDERR (integer)
//  Flag to ssh2_fetch_stream() requesting STDERR subchannel. 
  

	public String []getLoadedExtensions()
	{
		return new String[] { "ssh2" };
	}
	
    //resource ssh2_connect ( string $host [, int $port = 22 [, array $methods [, array $callbacks ]]] )
	public Value ssh2_connect(Env env, String hostname, @Optional Integer port, @Optional ArrayValue methods, @Optional ArrayValue callbacks)
	{
		JschSession jssh = new JschSession();
		jssh.jsch=new JSch();
		jssh.host = hostname;
		jssh.port = port == null ? 22 : port;
		
		return env.wrapJava(jssh, true);
	}

	//String ssh2_option ( resource $session , string $option , string $value )
	public String ssh2_option(Env env, Value psession, String option, @Optional String value)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();
		
		String ret = jssh.jsch.getConfig(option);
		if(value!=null)
		{
			jssh.jsch.setConfig(option, value);
		}
		return ret;
	}
	
    //mixed ssh2_auth_none ( resource $session , string $username )
	public boolean ssh2_auth_none(Env env, Value psession, String username)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();

		try
		{
			jssh.session = jssh.jsch.getSession(username, jssh.host, jssh.port);
			jssh.session.connect();
			return true;
		}
		catch(Exception ex)
		{
			log.severe(ex.toString());
		}
		return false;
	}

	//bool ssh2_auth_password ( resource $session , string $username , string $password )
	public boolean ssh2_auth_password(Env env, Value psession, String username, String password)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();

		try
		{
			jssh.session = jssh.jsch.getSession(username, jssh.host, jssh.port);
			jssh.session.setPassword(password);
			jssh.session.connect();
			return true;
		}
		catch(Exception ex)
		{
			log.severe(ex.toString());
		}
		return false;
	}
	
    //bool ssh2_auth_pubkey_file ( resource $session , string $username , string $pubkeyfile , string $privkeyfile [, string $passphrase ] )
	public boolean ssh2_auth_pubkey_file(Env env, Value psession, String username, String pubkeyfile, String privkeyfile, @Optional String passphrase)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();

		try
		{
			jssh.jsch.removeAllIdentity();
			
			if(passphrase==null)
			{
				jssh.jsch.addIdentity(privkeyfile, pubkeyfile, (byte[])null);
			}
			else
			{
				jssh.jsch.addIdentity(privkeyfile, pubkeyfile, passphrase.getBytes("UTF-8"));
			}
			jssh.session = jssh.jsch.getSession(username, jssh.host, jssh.port);
			jssh.session.connect();
			return true;
		}
		catch(Exception ex)
		{
			log.severe(ex.toString());
		}
		return false;
	}
	
	//resource ssh2_exec ( resource $session , string $command [, string $pty [, array $env [, int $width = 80 [, int $height = 25 [, int $width_height_type = SSH2_TERM_UNIT_CHARS ]]]]] )
	public JschChannel ssh2_exec(Env env, Value psession, String command, @Optional String pty, @Optional ArrayValue $env, @Optional Integer width, @Optional Integer height, @Optional Integer width_height_type)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();
	
		try
		{
			Channel channel=jssh.session.openChannel("exec");
		    ((ChannelExec)channel).setCommand(command);
		    channel.setInputStream(null);
		    channel.setOutputStream(null);
	
		    JschChannel chan = new JschChannel(env);
		    channel.connect();
		    chan.setChannel(channel);
		    return chan;
		}
		catch(Exception ex)
		{
			log.severe(ex.toString());
		}
		return null;
	}
	
	//void ssh2_close ( resource $session )
	public void ssh2_close(Env env, Value psession)
	{
		JschSession jssh = (JschSession)psession.toJavaObject();
		
		if(jssh.session!=null)
		{
			jssh.session.disconnect();
		}
	}


/*     
     bool ssh2_auth_hostbased_file ( resource $session , string $username , string $hostname , string $pubkeyfile , string $privkeyfile [, string $passphrase [, string $local_username ]] )
     
     
     
     
     
     
     resource ssh2_fetch_stream ( resource $channel , int $streamid )
     Fetches an alternate substream associated with an SSH2 channel stream. The SSH2 protocol currently defines only one substream, STDERR, which has a substream ID of SSH2_STREAM_STDERR (defined as 1). 
     
     string ssh2_fingerprint ( resource $session [, int $flags = SSH2_FINGERPRINT_MD5 | SSH2_FINGERPRINT_HEX ] )
     
     array ssh2_methods_negotiated ( resource $session )
     
     
     
     
     
     bool ssh2_publickey_add ( resource $pkey , string $algoname , string $blob [, bool $overwrite = false [, array $attributes ]] )
     
     resource ssh2_publickey_init ( resource $session )
     
     array ssh2_publickey_list ( resource $pkey )
     
     bool ssh2_publickey_remove ( resource $pkey , string $algoname , string $blob )
     
     
     
     
     bool ssh2_scp_recv ( resource $session , string $remote_file , string $local_file )
     
     bool ssh2_scp_send ( resource $session , string $local_file , string $remote_file [, int $create_mode = 0644 ] )
     
     array ssh2_sftp_lstat ( resource $sftp , string $path )
     
     bool ssh2_sftp_mkdir ( resource $sftp , string $dirname [, int $mode = 0777 [, bool $recursive = false ]] )
     
     string ssh2_sftp_readlink ( resource $sftp , string $link )
     
     string ssh2_sftp_realpath ( resource $sftp , string $filename )
     
     bool ssh2_sftp_rename ( resource $sftp , string $from , string $to )
     
     bool ssh2_sftp_rmdir ( resource $sftp , string $dirname )
     
     array ssh2_sftp_stat ( resource $sftp , string $path )
     
     bool ssh2_sftp_symlink ( resource $sftp , string $target , string $link )
     
     bool ssh2_sftp_unlink ( resource $sftp , string $filename )
     
     resource ssh2_sftp ( resource $session )
     
     resource ssh2_shell ( resource $session [, string $term_type = "vanilla" [, array $env [, int $width = 80 [, int $height = 25 [, int $width_height_type = SSH2_TERM_UNIT_CHARS ]]]]] )
     
     resource ssh2_tunnel ( resource $session , string $host , int $port )
     
	*/
}
