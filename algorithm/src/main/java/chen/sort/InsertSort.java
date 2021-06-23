package chen.sort;


import java.util.Arrays;


public class InsertSort {

    /**
     * 插入排序 , 升序
     * 思路:
     * 左侧为有序 (0-i), 右侧为无序
     * 每次选中i+1插叙有序列表
     *
     * @param arr
     */
    public static void InsertSort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int j = i - 1;
            int temp = arr[i];
            while (j >= 0 && arr[j] > temp) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = temp;
        }
    }

    public static void main(String[] args) {
        int [] arr = new int[]{4, 5, 7, 1, 29, 4, 4, 4, 4};
        InsertSort(arr);
        System.out.println(Arrays.toString(arr));
    }
}