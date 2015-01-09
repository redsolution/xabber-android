// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Well Known Services - Lists services offered by this host.
 *
 * @author Brian Wellington
 */

public class WKSRecord extends Record {

private static final long serialVersionUID = -9104259763909119805L;

public static class Protocol {
	/**
	 * IP protocol identifiers.  This is basically copied out of RFC 1010.
	 */

	private Protocol() {}

	/** Internet Control Message */
	public static final int ICMP = 1;

	/** Internet Group Management */
	public static final int IGMP = 2;

	/** Gateway-to-Gateway */
	public static final int GGP = 3;

	/** Stream */
	public static final int ST = 5;

	/** Transmission Control */
	public static final int TCP = 6;

	/** UCL */
	public static final int UCL = 7;

	/** Exterior Gateway Protocol */
	public static final int EGP = 8;

	/** any private interior gateway */
	public static final int IGP = 9;

	/** BBN RCC Monitoring */
	public static final int BBN_RCC_MON = 10;

	/** Network Voice Protocol */
	public static final int NVP_II = 11;

	/** PUP */
	public static final int PUP = 12;

	/** ARGUS */
	public static final int ARGUS = 13;

	/** EMCON */
	public static final int EMCON = 14;

	/** Cross Net Debugger */
	public static final int XNET = 15;

	/** Chaos */
	public static final int CHAOS = 16;

	/** User Datagram */
	public static final int UDP = 17;

	/** Multiplexing */
	public static final int MUX = 18;

	/** DCN Measurement Subsystems */
	public static final int DCN_MEAS = 19;

	/** Host Monitoring */
	public static final int HMP = 20;

	/** Packet Radio Measurement */
	public static final int PRM = 21;

	/** XEROX NS IDP */
	public static final int XNS_IDP = 22;

	/** Trunk-1 */
	public static final int TRUNK_1 = 23;

	/** Trunk-2 */
	public static final int TRUNK_2 = 24;

	/** Leaf-1 */
	public static final int LEAF_1 = 25;

	/** Leaf-2 */
	public static final int LEAF_2 = 26;

	/** Reliable Data Protocol */
	public static final int RDP = 27;

	/** Internet Reliable Transaction */
	public static final int IRTP = 28;

	/** ISO Transport Protocol Class 4 */
	public static final int ISO_TP4 = 29;

	/** Bulk Data Transfer Protocol */
	public static final int NETBLT = 30;

	/** MFE Network Services Protocol */
	public static final int MFE_NSP = 31;

	/** MERIT Internodal Protocol */
	public static final int MERIT_INP = 32;

	/** Sequential Exchange Protocol */
	public static final int SEP = 33;

	/** CFTP */
	public static final int CFTP = 62;

	/** SATNET and Backroom EXPAK */
	public static final int SAT_EXPAK = 64;

	/** MIT Subnet Support */
	public static final int MIT_SUBNET = 65;

	/** MIT Remote Virtual Disk Protocol */
	public static final int RVD = 66;

	/** Internet Pluribus Packet Core */
	public static final int IPPC = 67;

	/** SATNET Monitoring */
	public static final int SAT_MON = 69;

	/** Internet Packet Core Utility */
	public static final int IPCV = 71;

	/** Backroom SATNET Monitoring */
	public static final int BR_SAT_MON = 76;

	/** WIDEBAND Monitoring */
	public static final int WB_MON = 78;

	/** WIDEBAND EXPAK */
	public static final int WB_EXPAK = 79;

	private static Mnemonic protocols = new Mnemonic("IP protocol",
							 Mnemonic.CASE_LOWER);

	static {
		protocols.setMaximum(0xFF);
		protocols.setNumericAllowed(true);

		protocols.add(ICMP, "icmp");
		protocols.add(IGMP, "igmp");
		protocols.add(GGP, "ggp");
		protocols.add(ST, "st");
		protocols.add(TCP, "tcp");
		protocols.add(UCL, "ucl");
		protocols.add(EGP, "egp");
		protocols.add(IGP, "igp");
		protocols.add(BBN_RCC_MON, "bbn-rcc-mon");
		protocols.add(NVP_II, "nvp-ii");
		protocols.add(PUP, "pup");
		protocols.add(ARGUS, "argus");
		protocols.add(EMCON, "emcon");
		protocols.add(XNET, "xnet");
		protocols.add(CHAOS, "chaos");
		protocols.add(UDP, "udp");
		protocols.add(MUX, "mux");
		protocols.add(DCN_MEAS, "dcn-meas");
		protocols.add(HMP, "hmp");
		protocols.add(PRM, "prm");
		protocols.add(XNS_IDP, "xns-idp");
		protocols.add(TRUNK_1, "trunk-1");
		protocols.add(TRUNK_2, "trunk-2");
		protocols.add(LEAF_1, "leaf-1");
		protocols.add(LEAF_2, "leaf-2");
		protocols.add(RDP, "rdp");
		protocols.add(IRTP, "irtp");
		protocols.add(ISO_TP4, "iso-tp4");
		protocols.add(NETBLT, "netblt");
		protocols.add(MFE_NSP, "mfe-nsp");
		protocols.add(MERIT_INP, "merit-inp");
		protocols.add(SEP, "sep");
		protocols.add(CFTP, "cftp");
		protocols.add(SAT_EXPAK, "sat-expak");
		protocols.add(MIT_SUBNET, "mit-subnet");
		protocols.add(RVD, "rvd");
		protocols.add(IPPC, "ippc");
		protocols.add(SAT_MON, "sat-mon");
		protocols.add(IPCV, "ipcv");
		protocols.add(BR_SAT_MON, "br-sat-mon");
		protocols.add(WB_MON, "wb-mon");
		protocols.add(WB_EXPAK, "wb-expak");
	}

	/**
	 * Converts an IP protocol value into its textual representation
	 */
	public static String
	string(int type) {
		return protocols.getText(type);
	}

	/**
	 * Converts a textual representation of an IP protocol into its
	 * numeric code.  Integers in the range 0..255 are also accepted.
	 * @param s The textual representation of the protocol
	 * @return The protocol code, or -1 on error.
	 */
	public static int
	value(String s) {
		return protocols.getValue(s);
	}
}

public static class Service {
	/**
	 * TCP/UDP services.  This is basically copied out of RFC 1010,
	 * with MIT-ML-DEV removed, as it is not unique, and the description
	 * of SWIFT-RVF fixed.
	 */

	private Service() {}

	/** Remote Job Entry */
	public static final int RJE = 5;

	/** Echo */
	public static final int ECHO = 7;

	/** Discard */
	public static final int DISCARD = 9;

	/** Active Users */
	public static final int USERS = 11;

	/** Daytime */
	public static final int DAYTIME = 13;

	/** Quote of the Day */
	public static final int QUOTE = 17;

	/** Character Generator */
	public static final int CHARGEN = 19;

	/** File Transfer [Default Data] */
	public static final int FTP_DATA = 20;

	/** File Transfer [Control] */
	public static final int FTP = 21;

	/** Telnet */
	public static final int TELNET = 23;

	/** Simple Mail Transfer */
	public static final int SMTP = 25;

	/** NSW User System FE */
	public static final int NSW_FE = 27;

	/** MSG ICP */
	public static final int MSG_ICP = 29;

	/** MSG Authentication */
	public static final int MSG_AUTH = 31;

	/** Display Support Protocol */
	public static final int DSP = 33;

	/** Time */
	public static final int TIME = 37;

	/** Resource Location Protocol */
	public static final int RLP = 39;

	/** Graphics */
	public static final int GRAPHICS = 41;

	/** Host Name Server */
	public static final int NAMESERVER = 42;

	/** Who Is */
	public static final int NICNAME = 43;

	/** MPM FLAGS Protocol */
	public static final int MPM_FLAGS = 44;

	/** Message Processing Module [recv] */
	public static final int MPM = 45;

	/** MPM [default send] */
	public static final int MPM_SND = 46;

	/** NI FTP */
	public static final int NI_FTP = 47;

	/** Login Host Protocol */
	public static final int LOGIN = 49;

	/** IMP Logical Address Maintenance */
	public static final int LA_MAINT = 51;

	/** Domain Name Server */
	public static final int DOMAIN = 53;

	/** ISI Graphics Language */
	public static final int ISI_GL = 55;

	/** NI MAIL */
	public static final int NI_MAIL = 61;

	/** VIA Systems - FTP */
	public static final int VIA_FTP = 63;

	/** TACACS-Database Service */
	public static final int TACACS_DS = 65;

	/** Bootstrap Protocol Server */
	public static final int BOOTPS = 67;

	/** Bootstrap Protocol Client */
	public static final int BOOTPC = 68;

	/** Trivial File Transfer */
	public static final int TFTP = 69;

	/** Remote Job Service */
	public static final int NETRJS_1 = 71;

	/** Remote Job Service */
	public static final int NETRJS_2 = 72;

	/** Remote Job Service */
	public static final int NETRJS_3 = 73;

	/** Remote Job Service */
	public static final int NETRJS_4 = 74;

	/** Finger */
	public static final int FINGER = 79;

	/** HOSTS2 Name Server */
	public static final int HOSTS2_NS = 81;

	/** SU/MIT Telnet Gateway */
	public static final int SU_MIT_TG = 89;

	/** MIT Dover Spooler */
	public static final int MIT_DOV = 91;

	/** Device Control Protocol */
	public static final int DCP = 93;

	/** SUPDUP */
	public static final int SUPDUP = 95;

	/** Swift Remote Virtual File Protocol */
	public static final int SWIFT_RVF = 97;

	/** TAC News */
	public static final int TACNEWS = 98;

	/** Metagram Relay */
	public static final int METAGRAM = 99;

	/** NIC Host Name Server */
	public static final int HOSTNAME = 101;

	/** ISO-TSAP */
	public static final int ISO_TSAP = 102;

	/** X400 */
	public static final int X400 = 103;

	/** X400-SND */
	public static final int X400_SND = 104;

	/** Mailbox Name Nameserver */
	public static final int CSNET_NS = 105;

	/** Remote Telnet Service */
	public static final int RTELNET = 107;

	/** Post Office Protocol - Version 2 */
	public static final int POP_2 = 109;

	/** SUN Remote Procedure Call */
	public static final int SUNRPC = 111;

	/** Authentication Service */
	public static final int AUTH = 113;

	/** Simple File Transfer Protocol */
	public static final int SFTP = 115;

	/** UUCP Path Service */
	public static final int UUCP_PATH = 117;

	/** Network News Transfer Protocol */
	public static final int NNTP = 119;

	/** HYDRA Expedited Remote Procedure */
	public static final int ERPC = 121;

	/** Network Time Protocol */
	public static final int NTP = 123;

	/** Locus PC-Interface Net Map Server */
	public static final int LOCUS_MAP = 125;

	/** Locus PC-Interface Conn Server */
	public static final int LOCUS_CON = 127;

	/** Password Generator Protocol */
	public static final int PWDGEN = 129;

	/** CISCO FNATIVE */
	public static final int CISCO_FNA = 130;

	/** CISCO TNATIVE */
	public static final int CISCO_TNA = 131;

	/** CISCO SYSMAINT */
	public static final int CISCO_SYS = 132;

	/** Statistics Service */
	public static final int STATSRV = 133;

	/** INGRES-NET Service */
	public static final int INGRES_NET = 134;

	/** Location Service */
	public static final int LOC_SRV = 135;

	/** PROFILE Naming System */
	public static final int PROFILE = 136;

	/** NETBIOS Name Service */
	public static final int NETBIOS_NS = 137;

	/** NETBIOS Datagram Service */
	public static final int NETBIOS_DGM = 138;

	/** NETBIOS Session Service */
	public static final int NETBIOS_SSN = 139;

	/** EMFIS Data Service */
	public static final int EMFIS_DATA = 140;

	/** EMFIS Control Service */
	public static final int EMFIS_CNTL = 141;

	/** Britton-Lee IDM */
	public static final int BL_IDM = 142;

	/** Survey Measurement */
	public static final int SUR_MEAS = 243;

	/** LINK */
	public static final int LINK = 245;

	private static Mnemonic services = new Mnemonic("TCP/UDP service",
							Mnemonic.CASE_LOWER);

	static {
		services.setMaximum(0xFFFF);
		services.setNumericAllowed(true);

		services.add(RJE, "rje");
		services.add(ECHO, "echo");
		services.add(DISCARD, "discard");
		services.add(USERS, "users");
		services.add(DAYTIME, "daytime");
		services.add(QUOTE, "quote");
		services.add(CHARGEN, "chargen");
		services.add(FTP_DATA, "ftp-data");
		services.add(FTP, "ftp");
		services.add(TELNET, "telnet");
		services.add(SMTP, "smtp");
		services.add(NSW_FE, "nsw-fe");
		services.add(MSG_ICP, "msg-icp");
		services.add(MSG_AUTH, "msg-auth");
		services.add(DSP, "dsp");
		services.add(TIME, "time");
		services.add(RLP, "rlp");
		services.add(GRAPHICS, "graphics");
		services.add(NAMESERVER, "nameserver");
		services.add(NICNAME, "nicname");
		services.add(MPM_FLAGS, "mpm-flags");
		services.add(MPM, "mpm");
		services.add(MPM_SND, "mpm-snd");
		services.add(NI_FTP, "ni-ftp");
		services.add(LOGIN, "login");
		services.add(LA_MAINT, "la-maint");
		services.add(DOMAIN, "domain");
		services.add(ISI_GL, "isi-gl");
		services.add(NI_MAIL, "ni-mail");
		services.add(VIA_FTP, "via-ftp");
		services.add(TACACS_DS, "tacacs-ds");
		services.add(BOOTPS, "bootps");
		services.add(BOOTPC, "bootpc");
		services.add(TFTP, "tftp");
		services.add(NETRJS_1, "netrjs-1");
		services.add(NETRJS_2, "netrjs-2");
		services.add(NETRJS_3, "netrjs-3");
		services.add(NETRJS_4, "netrjs-4");
		services.add(FINGER, "finger");
		services.add(HOSTS2_NS, "hosts2-ns");
		services.add(SU_MIT_TG, "su-mit-tg");
		services.add(MIT_DOV, "mit-dov");
		services.add(DCP, "dcp");
		services.add(SUPDUP, "supdup");
		services.add(SWIFT_RVF, "swift-rvf");
		services.add(TACNEWS, "tacnews");
		services.add(METAGRAM, "metagram");
		services.add(HOSTNAME, "hostname");
		services.add(ISO_TSAP, "iso-tsap");
		services.add(X400, "x400");
		services.add(X400_SND, "x400-snd");
		services.add(CSNET_NS, "csnet-ns");
		services.add(RTELNET, "rtelnet");
		services.add(POP_2, "pop-2");
		services.add(SUNRPC, "sunrpc");
		services.add(AUTH, "auth");
		services.add(SFTP, "sftp");
		services.add(UUCP_PATH, "uucp-path");
		services.add(NNTP, "nntp");
		services.add(ERPC, "erpc");
		services.add(NTP, "ntp");
		services.add(LOCUS_MAP, "locus-map");
		services.add(LOCUS_CON, "locus-con");
		services.add(PWDGEN, "pwdgen");
		services.add(CISCO_FNA, "cisco-fna");
		services.add(CISCO_TNA, "cisco-tna");
		services.add(CISCO_SYS, "cisco-sys");
		services.add(STATSRV, "statsrv");
		services.add(INGRES_NET, "ingres-net");
		services.add(LOC_SRV, "loc-srv");
		services.add(PROFILE, "profile");
		services.add(NETBIOS_NS, "netbios-ns");
		services.add(NETBIOS_DGM, "netbios-dgm");
		services.add(NETBIOS_SSN, "netbios-ssn");
		services.add(EMFIS_DATA, "emfis-data");
		services.add(EMFIS_CNTL, "emfis-cntl");
		services.add(BL_IDM, "bl-idm");
		services.add(SUR_MEAS, "sur-meas");
		services.add(LINK, "link");
	}

	/**
	 * Converts a TCP/UDP service port number into its textual
	 * representation.
	 */
	public static String
	string(int type) {
		return services.getText(type);
	}

	/**
	 * Converts a textual representation of a TCP/UDP service into its
	 * port number.  Integers in the range 0..65535 are also accepted.
	 * @param s The textual representation of the service.
	 * @return The port number, or -1 on error.
	 */
	public static int
	value(String s) {
		return services.getValue(s);
	}
}
private byte [] address;
private int protocol;
private int [] services;

WKSRecord() {}

Record
getObject() {
	return new WKSRecord();
}

/**
 * Creates a WKS Record from the given data
 * @param address The IP address
 * @param protocol The IP protocol number
 * @param services An array of supported services, represented by port number.
 */
public
WKSRecord(Name name, int dclass, long ttl, InetAddress address, int protocol,
	  int [] services)
{
	super(name, Type.WKS, dclass, ttl);
	if (Address.familyOf(address) != Address.IPv4)
		throw new IllegalArgumentException("invalid IPv4 address");
	this.address = address.getAddress();
	this.protocol = checkU8("protocol", protocol);
	for (int i = 0; i < services.length; i++) {
		checkU16("service", services[i]);
	}
	this.services = new int[services.length];
	System.arraycopy(services, 0, this.services, 0, services.length);
	Arrays.sort(this.services);
}

void
rrFromWire(DNSInput in) throws IOException {
	address = in.readByteArray(4);
	protocol = in.readU8();
	byte [] array = in.readByteArray();
	List list = new ArrayList();
	for (int i = 0; i < array.length; i++) {
		for (int j = 0; j < 8; j++) {
			int octet = array[i] & 0xFF;
			if ((octet & (1 << (7 - j))) != 0) {
				list.add(new Integer(i * 8 + j));
			}
		}
	}
	services = new int[list.size()];
	for (int i = 0; i < list.size(); i++) {
		services[i] = ((Integer) list.get(i)).intValue();
	}
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	String s = st.getString();
	address = Address.toByteArray(s, Address.IPv4);
	if (address == null)
		throw st.exception("invalid address");

	s = st.getString();
	protocol = Protocol.value(s);
	if (protocol < 0) {
		throw st.exception("Invalid IP protocol: " + s);
	}

	List list = new ArrayList();
	while (true) {
		Tokenizer.Token t = st.get();
		if (!t.isString())
			break;
		int service = Service.value(t.value);
		if (service < 0) {
			throw st.exception("Invalid TCP/UDP service: " +
					   t.value);
		}
		list.add(new Integer(service));
	}
	st.unget();
	services = new int[list.size()];
	for (int i = 0; i < list.size(); i++) {
		services[i] = ((Integer) list.get(i)).intValue();
	}
}

/**
 * Converts rdata to a String
 */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(Address.toDottedQuad(address));
	sb.append(" ");
	sb.append(protocol);
	for (int i = 0; i < services.length; i++) {
		sb.append(" " + services[i]);
	}
	return sb.toString();
}

/**
 * Returns the IP address.
 */
public InetAddress
getAddress() {
	try {
		return InetAddress.getByAddress(address);
	} catch (UnknownHostException e) {
		return null;
	}
}

/**
 * Returns the IP protocol.
 */
public int
getProtocol() {
	return protocol;
}

/**
 * Returns the services provided by the host on the specified address.
 */
public int []
getServices() {
	return services;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(address);
	out.writeU8(protocol);
	int highestPort = services[services.length - 1];
	byte [] array = new byte[highestPort / 8 + 1];
	for (int i = 0; i < services.length; i++) {
		int port = services[i];
		array[port / 8] |= (1 << (7 - port % 8));
	}
	out.writeByteArray(array);
}

}
