package com.sharebill.core;

/** Một lần chuyển tiền: {@code from} trả cho {@code to} số tiền {@code amount}. */
public record Transfer(Long from, Long to, long amount) {
}
