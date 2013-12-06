## VirtualBox Host-Only Network Configuration

### VirtualBox UI

1.  Open VirtualBox Preferences/Settings
2.  Go to the **Network** tab.
3.  Click on **Host-only Networks**.
4.  Click the + icon to add a new network.
5.  Select the new network, _vboxnet**\<N\>**_, and click the Settings
    icon.
6.  Select the **Adapter** tab and provide the following settings:
  * **IPv4Address:** 192.168.33.1
  * **IPv4 Network Mask:** 255.255.255.0
  * **IPv6 Address:** \<blank\>
  * **IPv6 Network Mask Length:** 0
7.  Select the **DHCP Server** tab and provide the following settings:
  * Uncheck **Enable Server**
8.  Click **OK**
9.  If the newly created network is not **_vboxnet0_**, reconfigure the
    Lumify VM to use the new network:
  1.  Open the **Settings** for the **lumify-demo** VM.
  2.  Go to the **Network** tab.
  3.  Click on **Adapter 2**.
  4.  Select the newly created host-only network in the **Name:**
      selection box.
  5.  Click **OK**
  
### VBoxManage

```
ifname=$(VBoxManage hostonlyif create | awk '/Interface/ {print $2}' | sed -e "s/'//g")
VBoxManage hostonlyif ipconfig ${ifname} --ip 192.168.33.1
VBoxManage modifyvm lumify-demo --nic2 hostonly --hostonlyadapter2 ${ifname} 
```

**Note that the VirtualBox daemon may need to be restarted to
successfully add the network interfaces and default routes to the host
operating system.  If you cannot ping or connect to the Lumify VM,
please restart the daemon first and, if errors still occur, restart your
host system.**

For more information, please refer to the [Virtual
networking](https://www.virtualbox.org/manual/ch06.html) chapter of the
[VirtualBox manual](https://www.virtualbox.org/manual).

