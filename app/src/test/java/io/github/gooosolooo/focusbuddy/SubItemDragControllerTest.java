package io.github.gooosolooo.focusbuddy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 验证竖直拖动的落位边界，防止相邻项目在未到达落位位置时反复换位。 */
public class SubItemDragControllerTest {
    @Test
    public void staysInPlaceUntilReachingNeighborPlacement() {
        float[] centers = {24f, 88f, 152f};
        assertEquals(1, SubItemDragController.targetIndexForCenter(centers, 1, 24.1f));
        assertEquals(1, SubItemDragController.targetIndexForCenter(centers, 1, 151.9f));
        assertEquals(0, SubItemDragController.targetIndexForCenter(centers, 1, 24f));
        assertEquals(2, SubItemDragController.targetIndexForCenter(centers, 1, 152f));
    }

    @Test
    public void supportsSkippingMultipleRowsOfDifferentHeights() {
        float[] centers = {12f, 77f, 180f, 264f};
        assertEquals(0, SubItemDragController.targetIndexForCenter(centers, 2, 10f));
        assertEquals(1, SubItemDragController.targetIndexForCenter(centers, 2, 76f));
        assertEquals(3, SubItemDragController.targetIndexForCenter(centers, 0, 300f));
    }

    @Test
    public void supportsDraggingBackToOriginalPosition() {
        float[] centers = {24f, 88f, 152f};
        assertEquals(2, SubItemDragController.targetIndexForCenter(centers, 0, 160f));
        assertEquals(0, SubItemDragController.targetIndexForCenter(centers, 0, 24f));
        assertEquals(0, SubItemDragController.targetIndexForCenter(centers, 2, 10f));
        assertEquals(2, SubItemDragController.targetIndexForCenter(centers, 2, 152f));
    }

    @Test
    public void acceptsClampedFirstAndLastPlacementCenters() {
        // 高行移到矮行后，落位中心不等于矮行的原始中心。
        float[] placements = {40f, 88f, 136f};
        assertEquals(2, SubItemDragController.targetIndexForCenter(placements, 0, 136f));
        assertEquals(0, SubItemDragController.targetIndexForCenter(placements, 2, 40f));
    }
}
