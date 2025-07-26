package com.compiler.backend.location;

public class Location {
    private final LocationType type;
    private final String identifier; // 寄存器名/全局变量名
    private final int offset;        // 栈偏移量/立即数值

    // 寄存器构造器
    public static Location reg(String reg) {
        return new Location(LocationType.REGISTER, reg, 0);
    }

    // 栈偏移构造器
    public static Location stack(int offset) {
        return new Location(LocationType.STACK, "sp", offset);
    }

    // 全局变量构造器
    public static Location global(String name) {
        return new Location(LocationType.GLOBAL, name, 0);
    }

    // 立即数构造器
    public static Location imm(int value) {
        return new Location(LocationType.IMMEDIATE, null, value);
    }

    private Location(LocationType type, String id, int offset) {
        this.type = type;
        this.identifier = id;
        this.offset = offset;
    }

    // Getters...
    public LocationType getType() { return this.type; }
    public String getIdentifier() { return this.identifier; }
    public int getOffset() { return this.offset; }

    @Override
    public String toString() {
        return switch (type) {
            case REGISTER, GLOBAL -> identifier;
            case STACK -> offset + "(" + identifier + ")";
            case IMMEDIATE -> "" + offset;
        };
    }
}
