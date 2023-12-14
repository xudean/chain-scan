package org.pado;

/**
 * @Author xuda
 * @Date 2023/12/12 17:59
 */
public class Test {
    public static void main(String[] args) {
        Long start = 1092628L;
        Long end = 1177563L;
        Long gap = end - start;
        Long step = gap / 20;
        for (Long i = 0L; i < 20L; i++) {
            Long tempStart = start+i*step;
            Long tempEnd = start+(i+1)*step;
            System.out.println("curl http://127.0.0.1:38080/chain/block?start="+tempStart);
        }
    }
}
