package team302;

import battlecode.common.*;

public class MISSILE {
    public static boolean first = false;
    public static MapLocation toAttack = null;

    public static void execute(RobotController rc) throws GameActionException {
        if(rc.isCoreReady()){
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
            RobotInfo[] closeEnemies = rc.senseNearbyRobots(1, RobotPlayer.enemyTeam);
            RobotInfo[] closeHomies = rc.senseNearbyRobots(1, RobotPlayer.myTeam);
            boolean safeToBlow = true;
            boolean exploded = false;
            
            for(RobotInfo locEnemies : closeEnemies){
            	for(RobotInfo loc : closeHomies){
            		if (RobotPlayer.myLocation.distanceSquaredTo(loc.location) < 2){
            			if(enemyRobots.length > 0){
	                		charge(rc, loc.location);
	                		safeToBlow = false;
            			}else {
            				Util.tryMove(rc, RobotPlayer.myLocation.directionTo(loc.location).opposite());
            			}
                	}
            	}
            	if( safeToBlow && RobotPlayer.myLocation.distanceSquaredTo(locEnemies.location) < 2){
            		rc.explode();
            		exploded = true;
            		//System.out.println("ALLAH ALLAH JIHAAAAAAD!!!!!");
            		return;
            	}
            }
            if(!exploded && enemyRobots.length > 0){
	            for(RobotInfo loc : enemyRobots){
	            	charge(rc,  loc.location);
	            }
            }else {
            	Util.tryMove(rc, RobotPlayer.myLocation.directionTo(RobotPlayer.myHq).opposite());
            }
        }
    }
    
    private static void charge(RobotController rc, MapLocation location) throws GameActionException{
    	Util.tryMove(rc, RobotPlayer.myLocation.directionTo(location));
    }
    
}