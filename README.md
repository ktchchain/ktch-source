###########Environmental dependence
The project also MAVEN manages the JAR package, using springBoot as the container extension operation, JPA for the persistence layer, HQL for annotation, and Netty for network communication.
JAVA JDK1.8
MySQL 5.6

###########Deployment steps
1. Adding System Environment Variables
    Your server should support JDK1.8 runtime environment
	You can go to http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html to download jdk1.8
	Your server should support MySQL5.6 runtime environment



###########Directory structure description
├── Readme.md                   				// help
├── photon-chain-extend                         // Extension layer (place contract virtual machine and extended functions)
├── photon-chain-interfaces                     // Interface layer (places an open API)
│   ├── java						// source
│   ├── resources                	// config 
│  		├──application-mainnet.properties	// mainnet config
│		├──application-testnet.properties	// testnet config		
├── photon-chain-network						// Network communication and encryption layer (node communication blockchain logic)
├── photon-chain-storage                        // Storage layer (persistent block information)

###########Project Code Description
Dependency order  storage->network->extend->interfaces
![Image text](https://www-alphagdax-com.oss-cn-zhangjiakou.aliyuncs.com/2019/08/22/e87fbf59-75cf-48eb-b9af-c13f86eb1600.png)
The core of the block chain is placed under the Core directory.
![Image text](https://www-alphagdax-com.oss-cn-zhangjiakou.aliyuncs.com/2019/08/22/65f282d8-823f-45ce-a2ef-e93d370e887b.png)
FoundryMachine is the mining logic, there are comments in the code
GenesisBlock is Genesis Block Information
Initialization starts the content information that needs to be loaded for the node.
IterationData has been abandoned

MessageProcessor (important) message processing, pipeline block verification, are all inside, relying on peerhandler,
According to communication, it is divided into request information (REQProcessor), response information (RESPProcessor)
EVENT message event type resolution

NEW_BLOCK = 0;--New block
NEW_TRANSACTION = 1;--New water
SYNC_BLOCK = 2;--sync block
SYNC_TRANSACTION = 3;--Synchronized water
NODE_ADDRESS = 4;--Broadcast node address
SYNC_TOKEN = 5; - Synchronous token
NEW_TOKEN = 6;--New Token
PUSH_MAC = 7;--Push MAC address
ADD_PARTICIPANT = 8;--Add mining certificate
DEL_PARTICIPANT = 9;--Delete mining certificate
SYNC_PARTICIPANT = 10;--Synchronous mining certificate
SET_ZERO_PARTICIPANT=11;--Reset mining certificate

The network message transmission body uses GOOGLE's PROTO for message encapsulation, compression, encryption, and transmission.
The Proto file is located in the resources directory of the networok module.
![Image text](https://www-alphagdax-com.oss-cn-zhangjiakou.aliyuncs.com/2019/08/22/6a3c4e72-5ee1-492d-931b-5c8139adcb91.png)

ResetData resets the data, places the wrong data and illegal node data, and resets the reset and resets it.

SyncBlock block synchronization logic
SyncParticipant mining proof synchronization logic
SyncTimer Synchronization Timer Actuator
SyncToken Synchronize Token Information Logic
SyncUnconfirmedTran synchronization unacknowledged pipeline logic
Verification checker, flow check, block check logic
![Image text](https://www-alphagdax-com.oss-cn-zhangjiakou.aliyuncs.com/2019/08/22/bfd1dae0-e4eb-45f9-a6bf-32ce30d02316.png)

Extend extension layer
![Image text](https://www-alphagdax-com.oss-cn-zhangjiakou.aliyuncs.com/2019/08/22/b0974f84-4277-4ac9-a081-8addd9bbe30a.png)

Compiler stores pre-compiled logic with comments in the code. The basic logic is to compile the contract code into event code and opcode.
Universal stores the enumeration of event types, enumeration of opcodes, and enumeration of template contract types.
Vm stores the stack, program, virtual machine code
The stack is used to store variables, and the fixed variables are read in order.
The program class is used to put the op code to the PVM virtual machine, press the op step, the event step to perform the operation.
