package com.khi.chatservice.presentation.dto.res;

public record SliceInfo(
        boolean hasNext
) {
    public static SliceInfo of(boolean hasNext) {
        return new SliceInfo(hasNext);
    }
}