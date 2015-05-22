# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://atlas.hashicorp.com/search.
  #config.vm.box_url="https://atlas.hashicorp.com/nrel/CentOS-6.6-x86_64"

  config.vm.box = "nrel/CentOS-6.6-x86_64"

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Maybe this will make synced folder access faster ?
  #config.vm.synced_folder '.', '/vagrant', nfs: true

  # share the Maven repository so to avoid unnecessary downloading of maven dependencies
  config.vm.synced_folder "~/.m2", "/home/vagrant/.m2", create: true

  # Dev configuration
  config.vm.define 'dev', primary: true do |dev|
    config.vm.network :private_network, ip: "192.168.33.10"
    config.vm.hostname = "lumify-dev"
    config.vm.provider "virtualbox" do |vb|
      vb.name = "lumify-dev"
      vb.memory = 8192
      vb.cpus = 4
    end
    config.vm.provision "shell", inline: "sed -i 's/lumify-dev *//g' /etc/hosts"
    config.vm.provision "shell", inline: "echo \"192.168.33.10  lumify-dev\" >> /etc/hosts"
    config.vm.provision "shell", path: "vagrant/scripts/install-lumify-dependencies.sh"
  end

  # Demo configuration
  config.vm.define 'demo' do |demo|
    config.vm.network :private_network, ip: "192.168.33.12"
    # Create a forwarded port mapping which allows access to a specific port
    # within the machine from a port on the host machine. In the example below,
    # accessing "localhost:8080" will access port 80 on the guest machine.
    # config.vm.network "forwarded_port", guest: 80, host: 8080
    config.vm.network :forwarded_port, :guest => 8080, :host => 8080, :auto_correct => true
    config.vm.network :forwarded_port, :guest => 8443, :host => 8443, :auto_correct => true

    config.vm.hostname = "lumify-demo"
    config.vm.provider "virtualbox" do |vb|
      vb.name = "lumify-demo"
      vb.memory = 8192
      vb.cpus = 4
    end
    config.vm.network :forwarded_port, :guest => 8080, :host => 8080, :auto_correct => true
    config.vm.network :forwarded_port, :guest => 8443, :host => 8443, :auto_correct => true
    config.vm.provision "shell", inline: "sed -i 's/lumify-demo *//g' /etc/hosts"
    config.vm.provision "shell", inline: "echo \"192.168.33.12  lumify-demo\" >> /etc/hosts"
    config.vm.provision "shell", path: "vagrant/scripts/install-lumify-dependencies.sh"
    config.vm.provision "shell", inline: "cd /vagrant && mvn -P \"grunt unix\",web-war,web-war-with-gpw,web-war-with-ui-plugins clean package -DskipTests", privileged: false
    config.vm.provision "shell", path: "vagrant/scripts/install-lumify-demo.sh"
    config.vm.provision "shell", inline: "chkconfig --add jetty && service jetty start"
  end

  # Integration test configuration
#  config.vm.define 'itest' do |itest|
#    config.vm.network :private_network, ip: "192.168.33.11"
#    config.vm.hostname = "lumify-itest"
#    config.vm.provider "virtualbox" do |vb|
#      vb.name = "lumify-itest"
#      vb.memory = 8192
#      vb.cpus = 4
#    end
#    config.vm.network :forwarded_port, :guest => 8080, :host => 8080, :auto_correct => true
#    config.vm.network :forwarded_port, :guest => 8443, :host => 8443, :auto_correct => true
#    config.vm.provision "shell", inline: "sed -i 's/lumify-itest *//g' /etc/hosts"
#    config.vm.provision "shell", inline: "echo \"192.168.33.11  lumify-itest\" >> /etc/hosts"
#    config.vm.provision "shell", path: "vagrant/scripts/install-lumify-dependencies.sh"
#    config.vm.provision "file", source: "/vagrant/vagrant/dev/start.sh", destination: "/opt/start.sh"
#    config.vm.provision "shell", inline: "/bin/bash /opt/start.sh"
#  end
end
