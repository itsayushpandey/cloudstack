package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.PrepareUnmanageVMInstanceAnswer;
import com.cloud.agent.api.PrepareUnmanageVMInstanceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;

@ResourceWrapper(handles =  PrepareUnmanageVMInstanceCommand.class)
public final class LibvirtPrepareUnmanageVMInstanceCommandWrapper extends CommandWrapper<PrepareUnmanageVMInstanceCommand, PrepareUnmanageVMInstanceAnswer, LibvirtComputingResource> {
    private static final Logger s_logger = Logger.getLogger(LibvirtPrepareUnmanageVMInstanceCommandWrapper.class);
    @Override
    public PrepareUnmanageVMInstanceAnswer execute(PrepareUnmanageVMInstanceCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getInstanceName();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        s_logger.debug(String.format("Verify if KVM instance: [%s] is available before Unmanaging VM.", vmName));
        try {
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            final Domain domain = libvirtComputingResource.getDomain(conn, vmName);
            if (domain == null) {
                s_logger.error("Prepare Unmanage VMInstanceCommand: vm not found " + vmName);
                new PrepareUnmanageVMInstanceAnswer(command, false, String.format("Cannot find VM with name [%s] in KVM host.", vmName));
            }
        } catch (Exception e){
            s_logger.error("PrepareUnmanagedInstancesCommand failed due to " + e.getMessage());
            return new PrepareUnmanageVMInstanceAnswer(command, false, "Error: " + e.getMessage());
        }

        return new PrepareUnmanageVMInstanceAnswer(command, true, "OK");
    }
}
