// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: EventTypeEnum.proto

package com.photon.photonchain.network.proto;

public final class EventTypeEnum {
  private EventTypeEnum() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  /**
   * Protobuf enum {@code EventType}
   */
  public enum EventType
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>NEW_BLOCK = 0;</code>
     */
    NEW_BLOCK(0),
    /**
     * <code>NEW_TRANSACTION = 1;</code>
     */
    NEW_TRANSACTION(1),
    /**
     * <code>SYNC_BLOCK = 2;</code>
     */
    SYNC_BLOCK(2),
    /**
     * <code>SYNC_TRANSACTION = 3;</code>
     */
    SYNC_TRANSACTION(3),
    /**
     * <code>NODE_ADDRESS = 4;</code>
     */
    NODE_ADDRESS(4),
    /**
     * <code>SYNC_TOKEN = 5;</code>
     */
    SYNC_TOKEN(5),
    /**
     * <code>NEW_TOKEN = 6;</code>
     */
    NEW_TOKEN(6),
    /**
     * <code>PUSH_MAC = 7;</code>
     */
    PUSH_MAC(7),
    /**
     * <code>ADD_PARTICIPANT = 8;</code>
     */
    ADD_PARTICIPANT(8),
    /**
     * <code>DEL_PARTICIPANT = 9;</code>
     */
    DEL_PARTICIPANT(9),
    /**
     * <code>SYNC_PARTICIPANT = 10;</code>
     */
    SYNC_PARTICIPANT(10),
    /**
     * <code>SET_ZERO_PARTICIPANT = 11;</code>
     */
    SET_ZERO_PARTICIPANT(11),
    /**
     * <code>NEW_CONTRACT = 12;</code>
     */
    NEW_CONTRACT(12),
    /**
     * <code>CANCEL_CONTRACT = 13;</code>
     */
    CANCEL_CONTRACT(13),
    /**
     * <code>IS_CANCEL = 14;</code>
     */
    IS_CANCEL(14),
    /**
     * <code>DeadLine_vote = 15;</code>
     */
    DeadLine_vote(15),
    ;

    /**
     * <code>NEW_BLOCK = 0;</code>
     */
    public static final int NEW_BLOCK_VALUE = 0;
    /**
     * <code>NEW_TRANSACTION = 1;</code>
     */
    public static final int NEW_TRANSACTION_VALUE = 1;
    /**
     * <code>SYNC_BLOCK = 2;</code>
     */
    public static final int SYNC_BLOCK_VALUE = 2;
    /**
     * <code>SYNC_TRANSACTION = 3;</code>
     */
    public static final int SYNC_TRANSACTION_VALUE = 3;
    /**
     * <code>NODE_ADDRESS = 4;</code>
     */
    public static final int NODE_ADDRESS_VALUE = 4;
    /**
     * <code>SYNC_TOKEN = 5;</code>
     */
    public static final int SYNC_TOKEN_VALUE = 5;
    /**
     * <code>NEW_TOKEN = 6;</code>
     */
    public static final int NEW_TOKEN_VALUE = 6;
    /**
     * <code>PUSH_MAC = 7;</code>
     */
    public static final int PUSH_MAC_VALUE = 7;
    /**
     * <code>ADD_PARTICIPANT = 8;</code>
     */
    public static final int ADD_PARTICIPANT_VALUE = 8;
    /**
     * <code>DEL_PARTICIPANT = 9;</code>
     */
    public static final int DEL_PARTICIPANT_VALUE = 9;
    /**
     * <code>SYNC_PARTICIPANT = 10;</code>
     */
    public static final int SYNC_PARTICIPANT_VALUE = 10;
    /**
     * <code>SET_ZERO_PARTICIPANT = 11;</code>
     */
    public static final int SET_ZERO_PARTICIPANT_VALUE = 11;
    /**
     * <code>NEW_CONTRACT = 12;</code>
     */
    public static final int NEW_CONTRACT_VALUE = 12;
    /**
     * <code>CANCEL_CONTRACT = 13;</code>
     */
    public static final int CANCEL_CONTRACT_VALUE = 13;
    /**
     * <code>IS_CANCEL = 14;</code>
     */
    public static final int IS_CANCEL_VALUE = 14;
    /**
     * <code>DeadLine_vote = 15;</code>
     */
    public static final int DeadLine_vote_VALUE = 15;


    public final int getNumber() {
      return value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static EventType valueOf(int value) {
      return forNumber(value);
    }

    public static EventType forNumber(int value) {
      switch (value) {
        case 0: return NEW_BLOCK;
        case 1: return NEW_TRANSACTION;
        case 2: return SYNC_BLOCK;
        case 3: return SYNC_TRANSACTION;
        case 4: return NODE_ADDRESS;
        case 5: return SYNC_TOKEN;
        case 6: return NEW_TOKEN;
        case 7: return PUSH_MAC;
        case 8: return ADD_PARTICIPANT;
        case 9: return DEL_PARTICIPANT;
        case 10: return SYNC_PARTICIPANT;
        case 11: return SET_ZERO_PARTICIPANT;
        case 12: return NEW_CONTRACT;
        case 13: return CANCEL_CONTRACT;
        case 14: return IS_CANCEL;
        case 15: return DeadLine_vote;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<EventType>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        EventType> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<EventType>() {
            public EventType findValueByNumber(int number) {
              return EventType.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.photon.photonchain.network.proto.EventTypeEnum.getDescriptor().getEnumTypes().get(0);
    }

    private static final EventType[] VALUES = values();

    public static EventType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private EventType(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:EventType)
  }


  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\023EventTypeEnum.proto*\267\002\n\tEventType\022\r\n\tN" +
      "EW_BLOCK\020\000\022\023\n\017NEW_TRANSACTION\020\001\022\016\n\nSYNC_" +
      "BLOCK\020\002\022\024\n\020SYNC_TRANSACTION\020\003\022\020\n\014NODE_AD" +
      "DRESS\020\004\022\016\n\nSYNC_TOKEN\020\005\022\r\n\tNEW_TOKEN\020\006\022\014" +
      "\n\010PUSH_MAC\020\007\022\023\n\017ADD_PARTICIPANT\020\010\022\023\n\017DEL" +
      "_PARTICIPANT\020\t\022\024\n\020SYNC_PARTICIPANT\020\n\022\030\n\024" +
      "SET_ZERO_PARTICIPANT\020\013\022\020\n\014NEW_CONTRACT\020\014" +
      "\022\023\n\017CANCEL_CONTRACT\020\r\022\r\n\tIS_CANCEL\020\016\022\021\n\r" +
      "DeadLine_vote\020\017B5\n$com.photon.photonchai" +
      "n.network.protoB\rEventTypeEnum"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
