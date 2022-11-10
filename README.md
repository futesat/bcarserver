1) Install Raspian with Raspberry Pi Imager 

2) JAVA install (With sdkman)

curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 11.0.16-zulu

3) Configura raspberry as Access Point

 sudo apt-get install iptables
 
git clone https://github.com/idev1/rpihotspot.git

cd rpihotspot

sudo chmod +x setup-network.sh

sudo ./setup-network.sh --install --ap-ssid="BOSKICAR" --ap-password="<SET YOUR PASSWORD HERE>" --ap-password-encrypt --ap-country-code="ES" --ap-ip-address="192.168.0.1" --wifi-interface="wlan0"

4) Install pi-blaster

git clone https://github.com/sarfata/pi-blaster.git
sudo apt-get install autoconf
./autogen.sh 
./configure
sudo make install

Create cfg file: /etc/default/pi-blaster with the content:

DAEMON_OPTS="--gpio 13,14,24,26"

5) Install pi4j & wiringpi

curl -sSL https://pi4j.com/install | sudo bash
sudo pi4j --wiringpi

6) Compile JAR & copy bcarserver JAR:
Copiar en ruta: /usr/local/bin/bcarserver-1.0.1-RELEASE.jar

7) Create systemd service -> /etc/systemd/system/boskicar.service

sudo systemctl daemon-reload 
sudo systemctl enable
sudo service boskicar start

8) Check application logs

sudo journalctl --unit=boskicar -n 50 --no-pager

9) Check application API: 

curl -v -H 'Content-Type: application/json' http://192.168.0.1:3333/status

curl -v -X POST -H 'Content-Type: application/json' http://192.168.0.1:3333/mobilecontrol/ON

curl -v -X POST -H 'Content-Type: application/json' http://192.168.0.1:3333/shutdown

10) Raspberry cleanning (headless)

wget "https://raw.githubusercontent.com/dumbo25/unsed_rpi/main/unused_rpi.sh"
sudo chmod +x unused_rpi.sh
sudo bash unused_rpi.sh
sudo apt remove --purge cups
sudo apt remove --purge pulseaudio
udo apt-get purge bluez -y
sudo apt autoremove
sudo apt clean
sudo reboot

11) RO FileSystem

sudo raspi-config

Performance Options > Overlay File System 

12) Backup & Restore

sudo dd if=/dev/disk2 of=boskicarOS.dmg
sudo dd if=boskicarOS.dmg of=/dev/disk2
