// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.network.contrail.model;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.apache.cloudstack.network.contrail.management.ContrailManagerImpl;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

import junit.framework.TestCase;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.types.VirtualMachineInterface;

public class VMInterfaceModelTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(VMInterfaceModelTest.class);

    @Test
    public void testCreateVMInterface() throws IOException {

        String uuid;
        ContrailManagerImpl contrailMgr = mock(ContrailManagerImpl.class);
        ModelController controller = mock(ModelController.class);
        ApiConnector api = new ApiConnectorMock(null, 0);
        when(controller.getManager()).thenReturn(contrailMgr);
        when(controller.getApiAccessor()).thenReturn(api);

        // Create Virtual-Network (VN)
        NetworkVO network = MockNetworkVO.getNetwork(Network.State.Implemented);
        NetworkDao networkDao = mock(NetworkDao.class);
        when(networkDao.findById(anyLong())).thenReturn(network);
        when(controller.getNetworkDao()).thenReturn(networkDao);

        when(contrailMgr.getCanonicalName(network)).thenReturn("testnetwork");
        when(contrailMgr.getProjectId(network.getDomainId(), network.getAccountId())).thenReturn("testProjectId");

        VirtualNetworkModel vnModel = new VirtualNetworkModel(network, UUID.randomUUID().toString(),
                "test", TrafficType.Guest);
        vnModel.build(controller, network);
        try {
            vnModel.update(controller);
        } catch (Exception ex) {
            fail("virtual-network update failed ");
        }

        // Create Virtual-Machine (VM)
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getInstanceName()).thenReturn("testVM1");
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getDomainId()).thenReturn(10L);
        when(vm.getAccountId()).thenReturn(42L);
        UserVmDao VmDao = mock(UserVmDao.class);
        when(VmDao.findById(anyLong())).thenReturn(null);
        when(controller.getVmDao()).thenReturn(VmDao);

        VirtualMachineModel vmModel = new VirtualMachineModel(vm, UUID.randomUUID().toString());
        vmModel.build(controller, vm);
        try {
            vmModel.update(controller);
        } catch (Exception ex) {
            fail("virtual-machine update failed ");
        }

        // Create Virtual=Machine-Interface (VMInterface)
        NicVO nic = mock(NicVO.class);
        when(nic.getIPv4Address()).thenReturn("10.1.1.2");
        when(nic.getMacAddress()).thenReturn("00:01:02:03:04:05");
        when(nic.getDeviceId()).thenReturn(100);
        when(nic.getState()).thenReturn(NicVO.State.Allocated);
        when(nic.getNetworkId()).thenReturn(10L);

        when(contrailMgr.getVifNameByVmName(anyString(), anyInt())).thenReturn("testVM1-100");

        uuid = UUID.randomUUID().toString();
        VMInterfaceModel vmiModel = new VMInterfaceModel(uuid);

        assertEquals(uuid, vmiModel.getUuid());

        vmiModel.addToVirtualMachine(vmModel);
        vmiModel.addToVirtualNetwork(vnModel);

        try {
            vmiModel.build(controller, vm, nic);
            vmiModel.setActive();
        } catch (Exception ex) {
            fail("vm-interface build failed ");
        }

        try {
            vmiModel.update(controller);
        } catch (Exception ex) {
            fail("vm-interface update failed ");
        }

        // Verify virtual-machine-interface creation.
        try {
            VirtualMachineInterface vmi = (VirtualMachineInterface) api.findById(VirtualMachineInterface.class, vmiModel.getUuid());
            assertNotNull(vmi);
        } catch (Exception ex) {
            fail("vm-interface verify failed ");
        }
    }
}
