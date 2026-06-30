package com.memopoly.network.packets;

/**
 * Пакет отклонения действия: отправляется сервером клиенту в случае невалидного хода с указанием причины.
 */
public class ActionRejectedPacket {
    public String actionType;
    public String reasonCode;
    public String reason;

    public ActionRejectedPacket() {
    }

    public ActionRejectedPacket(String actionType, String reasonCode, String reason) {
        this.actionType = actionType;
        this.reasonCode = reasonCode;
        this.reason = reason;
    }
}
