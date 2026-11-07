package com.fonzzscripter.staffjobsui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Carries the existing StaffJobs plugin-message byte stream without changing its wire format. */
public record StaffJobsPayload(byte[] data) implements CustomPayload {
    public static final Id<StaffJobsPayload> ID = new Id<>(Identifier.of("staffjobs", "ui"));
    public static final PacketCodec<RegistryByteBuf, StaffJobsPayload> CODEC = PacketCodec.of(
            (buf, payload) -> buf.writeBytes(payload.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new StaffJobsPayload(bytes);
            }
    );

    public StaffJobsPayload {
        if (data == null || data.length > 1024) {
            throw new IllegalArgumentException("Invalid StaffJobs payload size");
        }
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
