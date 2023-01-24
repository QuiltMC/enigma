package cuchaz.enigma.inputs.bridge;

// c extends a
public class SubClass extends BaseClass {
    // <init>(III)V
    public SubClass(int x, int y, int z) {
        super(x, y, z);
    }

    // f()Lc;
    // bridge d()La;
    public SubClass foo() {
        System.out.println("bar");
        return this;
    }

    // d(I)Lc;
    // bridge a(I)La;
    public SubClass foo(int x) {
        return null;
    }

    // c(II)Lc;
    // bridge a(II)La;
    public SubClass foo(int x, int y) {
        return baz(y);
    }

    // g()Lc;
    // bridge e()La;
    public SubClass bar() {
        return new SubClass(getX(), -1, 0);
    }

    // e(I)Lc;
    // bridge b(I)La;
    public SubClass bar(int x) {
        return baz(-1, x);
    }

    // f(I)Lc;
    // bridge c(I)La;
    public SubClass baz(int xz) {
        return new SubClass(xz, getY(), getZ() + xz);
    }

    // d(II)Lc;
    // bridge b(II)La;
    public SubClass baz(int xz, int y) {
        if (y == 0) {
            return this;
        }
        return new SubClass(getX() - xz, getY() * y, getZ() + xz);
    }

    // c$a extends c
    public static class InnerSubClass extends SubClass {
        // <init>(III)V
        public InnerSubClass(int x, int y, int z) {
            super(x, y, z);
        }

        // bridge d()La;
        // bridge a(I)La;
        // bridge a(II)La;
        // bridge e()La;
        // bridge b(I)La;
        // bridge c(I)La;
        // bridge b(II)La;
    }
}
