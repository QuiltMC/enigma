package cuchaz.enigma.inputs.bridge;

// a
public class BaseClass {
    // a
    private int x;
    // b
    private int y;
    // c
    private int z;

    // <init>(III)V
    public BaseClass(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // a()I
    public int getX() {
        return this.x;
    }

    // b()I
    public int getY() {
        return this.y;
    }

    // c()I
    public int getZ() {
        return this.z;
    }

    // d()La;
    public BaseClass foo() {
        System.out.println("foo");
        return this;
    }

    // a(I)La;
    public BaseClass foo(int x) {
        return new BaseClass(x, 0, 0);
    }

    // a(II)La;
    public BaseClass foo(int x, int y) {
        return bar(1);
    }

    // e()La;
    public BaseClass bar() {
        return new BaseClass(0, 0, 0);
    }

    // b(I)La;
    public BaseClass bar(int x) {
        return baz(1, x);
    }

    // c(I)La;
    public BaseClass baz(int xz) {
        return new BaseClass(xz, 0, xz);
    }

    // b(II)La;
    public BaseClass baz(int xz, int y) {
        if (y == 0) {
            return this;
        }
        return new BaseClass(getX(), y, xz);
    }
}
