package com.example.dishroulette;
import org.junit.Test;
import static org.junit.Assert.*;
public class InspectionStateTest {
    @Test public void heldAngleIsContinuousAndRestingPositionsMapToStableSlots(){
        InspectionState state=new InspectionState();state.cylinderAngle=27.25f;
        assertEquals(27.25f,state.cylinderAngle,.001f);
        assertEquals(0,InspectionState.restingAngle(state.cylinderAngle,0),.001f);
        assertEquals(60,InspectionState.restingAngle(32,0),.001f);
        assertEquals(5,InspectionState.chamber(60));assertEquals(1,InspectionState.chamber(-60));assertEquals(1,InspectionState.chamber(-420));
    }
    @Test public void momentumIsLimitedAndAlignmentTakesTheShortPath(){
        assertEquals(60,InspectionState.restingAngle(18,500),.001f);
        assertEquals(120,InspectionState.restingAngle(45,5000),.001f);
        assertEquals(360,InspectionState.alignedAngle(350,0),.001f);
        assertEquals(-60,InspectionState.alignedAngle(0,1),.001f);
    }
    @Test public void cameraSafetyAndResetDoNotResetCylinderOrIngredientSelection(){
        InspectionState state=new InspectionState();state.cylinderAngle=-120;
        state.orbit(1000,-1000);assertEquals(82,state.yaw,.001f);assertEquals(-58,state.pitch,.001f);
        state.scale(100);assertEquals(.24f,state.zoom,.001f);state.scale(.00001f);assertEquals(1.8f,state.zoom,.001f);
        state.scale(Float.NaN);assertEquals(1.8f,state.zoom,.001f);
        state.resetCamera();assertEquals(1,state.zoom,.001f);assertEquals(0,state.yaw,.001f);assertEquals(-120,state.cylinderAngle,.001f);
    }
}
