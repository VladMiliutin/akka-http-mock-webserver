package xyz.vladm.akka_mock_server

/**
 * I know that native sort is much better, but just want to keep memory on the Algorithm
 */
class QuickSort {
    companion object {
        fun <T> sort(items: ArrayList<T>, comparator: Comparator<T>) {
            sort(items, 0, items.size - 1, comparator)
        }

        private fun <T> sort(items: ArrayList<T>, start: Int, end: Int, comparator: Comparator<T>) {
            if (items.size < 2) return

            if (start < end) {
                val pivotIndex = partition(items, start, end, comparator)
                sort(items, pivotIndex, end, comparator)
                sort(items, start + 1, pivotIndex, comparator)
            }
        }

        private fun <T> partition(items: ArrayList<T>, start: Int, end: Int, comparator: Comparator<T>): Int {
            val pivot = items[end]

            var i = (start - 1)

            for (j in start until end) {
                if (comparator.compare(items[j], pivot) <= -1) {
                    i++;
                    swap(items, i, j)
                }
            }

            swap(items, i + 1, end)
            return i + 1
        }

        private fun <T> swap(items: ArrayList<T>, i: Int, j: Int) {
            val tmp = items[i];
            items[i] = items[j]
            items[j] = tmp;
        }
    }
}