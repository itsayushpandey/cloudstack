package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;

import java.util.HashMap;

@ResourceWrapper(handles= GetUnmanagedInstancesCommand.class)
public final class LibvirtGetUnmanagedInstancesCommandWrapper extends CommandWrapper<GetUnmanagedInstancesCommand, GetUnmanagedInstancesAnswer, LibvirtComputingResource> {
    private static final Logger s_logger = Logger.getLogger(LibvirtPrepareUnmanageVMInstanceCommandWrapper.class);

    @Override
    public GetUnmanagedInstancesAnswer execute(GetUnmanagedInstancesCommand command, LibvirtComputingResource libvirtComputingResource) {
        s_logger.info("Need to implement business logic");

        HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
        try {
            final String vmName = command.getInstanceName();
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            final Domain domain = libvirtComputingResource.getDomain(conn, vmName);

            // TODO: Ayush: create UnmanagedInstanceTO from domain
            // Need to ask if domain can be template or not like in VMWare

            if (domain == null) {
                s_logger.error("GetUnmanagedInstancesCommand: vm not found " + vmName);
                throw new CloudRuntimeException("GetUnmanagedInstancesCommand: vm not found " + vmName);
            }

            // Filter managed instances
            if (command.hasManagedInstance(domain.getName())) {
                s_logger.error("GetUnmanagedInstancesCommand: vm already managed " + vmName);
                throw new CloudRuntimeException("GetUnmanagedInstancesCommand:  vm already managed " + vmName);
            }

            // Filter instance if answer is requested for a particular instance name
            if (StringUtils.isNotEmpty(command.getInstanceName()) &&
                    !command.getInstanceName().equals(domain.getName())) {
                s_logger.error("GetUnmanagedInstancesCommand: exact vm name not found " + vmName);
                throw new CloudRuntimeException("GetUnmanagedInstancesCommand: exact vm name not found " + vmName);
            }
            UnmanagedInstanceTO instance = getUnmanagedInstance(libvirtComputingResource, domain);
            unmanagedInstances.put(instance.getName(), instance);
        } catch (Exception e) {
            s_logger.error("GetUnmanagedInstancesCommand failed due to " + e.getMessage());
            throw new CloudRuntimeException("GetUnmanagedInstancesCommand failed due to " + e.getMessage());
        }
        return new GetUnmanagedInstancesAnswer(command, "True", unmanagedInstances);
    }

    private UnmanagedInstanceTO getUnmanagedInstance(LibvirtComputingResource libvirtComputingResource, Domain domain) {
        try {
            final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
            parser.parseDomainXML(domain.getXMLDesc(0));

            final UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
            instance.setName(domain.getName());
            instance.setCpuCores((int) LibvirtComputingResource.countDomainRunningVcpus(domain));
            instance.setCpuCoresPerSocket(parser.getCpuModeDef().getCoresPerSocket());
            instance.setCpuSpeed(parser.getCpuTuneDef().getShares());
            instance.setMemory((int) LibvirtComputingResource.getDomainMemory(domain));

            // TODO: Ayush complete this function.
//            instance.setOperatingSystemId(domain.getVmGuestInfo().getGuestId());
//            if (StringUtils.isEmpty(instance.getOperatingSystemId())) {
//                instance.setOperatingsSystemId(domain.getConfigSummary().getGuestId());
//                instance.setOperatingSystemId(domain.getOSType());
//            }
//            VirtualMachineGuestOsIdentifier osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST;
//            try {
//                osIdentifier = VirtualMachineGuestOsIdentifier.fromValue(instance.getOperatingSystemId());
//            } catch (IllegalArgumentException iae) {
//                if (StringUtils.isNotEmpty(instance.getOperatingSystemId()) && instance.getOperatingSystemId().contains("64")) {
//                    osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
//                }
//            }
//            instance.setOperatingSystem(domain.getGuestInfo().getGuestFullName());
//            if (StringUtils.isEmpty(instance.getOperatingSystem())) {
//                instance.setOperatingSystem(domain.getConfigSummary().getGuestFullName());
//            }
//            UnmanagedInstanceTO.PowerState powerState = UnmanagedInstanceTO.PowerState.PowerUnknown;
//            if (domain.getPowerState().toString().equalsIgnoreCase("POWERED_ON")) {
//                powerState = UnmanagedInstanceTO.PowerState.PowerOn;
//            }
//            if (domain.getPowerState().toString().equalsIgnoreCase("POWERED_OFF")) {
//                powerState = UnmanagedInstanceTO.PowerState.PowerOff;
//            }
//            instance.setPowerState(powerState);
//            instance.setDisks(getUnmanageInstanceDisks(domain));
//            instance.setNics(getUnmanageInstanceNics(hyperHost, domain));
            return instance;
        } catch (Exception e) {
            s_logger.info("Unable to retrieve unmanaged instance info. " + e.getMessage());
            throw new CloudRuntimeException("Unable to retrieve unmanaged instance info. " + e.getMessage());
        }
    }

}
