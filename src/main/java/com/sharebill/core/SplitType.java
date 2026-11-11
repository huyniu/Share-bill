package com.sharebill.core;

/** Cách chia một khoản chi cho các thành viên. */
public enum SplitType {
    /** Chia đều cho những người tham gia. */
    EQUAL,
    /** Mỗi người chịu một số tiền cụ thể; tổng phải bằng số tiền hóa đơn. */
    EXACT,
    /** Chia theo số phần (ví dụ: A 2 phần, B 1 phần). */
    SHARES
}
