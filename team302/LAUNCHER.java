package team302;

import battlecode.common.*;

import java.lang.System;

public class LAUNCHER {
    public static RobotController rc;
    public static RobotType[] canSpawn = {RobotType.MISSILE};
    public static boolean atGoal = false;
    public static MapLocation goal = null;
    public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    public static int numTowers;
    public static MapLocation target;

    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        int executeStartRound = Clock.getRoundNum();
        
        if (rc.isCoreReady()) {
        	if(Util.shouldFlee(rc, RobotPlayer.myLocation, false)){
    			Util.fleeNew(rc);
    		}else {
	            RobotInfo[] enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
	            if(rc.getMissileCount() == 5) {
	            	if(enemyRobots.length > 0) {
	            		Direction target = RobotPlayer.myLocation.directionTo(enemyRobots[RobotPlayer.rand.nextInt(enemyRobots.length)].location);
	            		//System.out.println(enemyRobots.length);
	            		if (rc.canLaunch(target)) {
	            			rc.launchMissile(target);
	            		}
	            	}
	            	else {
	            		Direction dir = Bug.startBuggin(rc,RobotPlayer.myTowers[RobotPlayer.rand.nextInt(RobotPlayer.myTowers.length)], 0);
	            		if(rc.canMove(dir)){
	            			rc.move(dir);
	            		}
	            	}
	            }else {
	            	Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 3);
	            	if(rc.canMove(dir)){
	            		rc.move(dir);
	            	}
	            }
	        }        
	    }
	}
}