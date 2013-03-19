package com.xabber.android.data.extension.version;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;

import android.os.Build;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.androiddev.R;
import com.xabber.xmpp.version.Version;

/**
 * Response to version requests with the client software name, version and OS
 * 
 * http://xmpp.org/extensions/xep-0092.html
 * 
 * @author Wolfgang Wermund
 *
 */
public class VersionManager implements OnPacketListener {

	private static final String FEATURE = "jabber:iq:version";
	
	private static final VersionManager instance;
	
	static {
		instance = new VersionManager();
		Application.getInstance().addManager(instance);

		Connection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					@Override
					public void connectionCreated(final Connection connection) {
						ServiceDiscoveryManager.getInstanceFor(connection)
								.addFeature(FEATURE);
					}
				});
	}
	
	public static VersionManager getInstance() {
		return instance;
	}
	
	private VersionManager() {}
	
	@Override
	public void onPacket(ConnectionItem connection, String bareAddress,
			Packet packet) {
		if (!(connection instanceof AccountItem))
			return;
		if (!(packet instanceof Version))
			return;
		Version input = (Version) packet;
		final String account = ((AccountItem) connection).getAccount();
		if(input.getType() == IQ.Type.GET) {
			Version version = new Version();
			version.setPacketID(input.getPacketID());
			version.setFrom(input.getTo());
			version.setTo(input.getFrom());
			version.setType(IQ.Type.RESULT);
			
			version.setName(Application.getInstance().getString(R.string.application_name));
			version.setOs("Android " + Build.VERSION.RELEASE);
			version.setVersion(Application.getInstance().getString(R.string.application_version));
			try {
				ConnectionManager.getInstance().sendPacket(account, version);
			} catch (NetworkException e) {
				LogManager.exception(this, e);
			}
		} else {
			return;
		}
		
	}

}
