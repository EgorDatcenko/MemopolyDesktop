package com.memopoly.utils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {
    public static void copyToClipboard(String text) {
        try {
            StringSelection stringSelection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            AppLog.info("Clipboard", "Скопировано в буфер обмена: " + text);
        } catch (Exception e) {
            AppLog.warn("Clipboard", "Ошибка копирования: " + e.getMessage());
        }
    }
}
