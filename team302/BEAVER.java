package team302;

import battlecode.common.*;

import java.util.Dictionary;


public class BEAVER {
    public static RobotController rc;
	public static int[] targetPriority = MyConstants.cowardPriorities;
    public static RobotType[] canBuild = {RobotType.MINERFACTORY,
                                          RobotType.BARRACKS,
                                          RobotType.TANKFACTORY,
                                          RobotType.HELIPAD,
                                          RobotType.AEROSPACELAB,
                                          RobotType.TECHNOLOGYINSTITUTE,
                                          RobotType.TRAININGFIELD,
                                          RobotType.SUPPLYDEPOT,
                                          RobotType.HANDWASHSTATION};
	public static boolean firstSupplied = false;

    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        if (rc.getRoundLimit() - Clock.getRoundNum() < 100) {
            Util.yoloBitches(rc);
        }

        if (rc.isCoreReady()) {

			if(!firstSupplied) {
				if(rc.getSupplyLevel() >= 1000) {
					firstSupplied = true;
				}
                Util.requestSupply(rc, 1000, rc.getLocation());
			}
			
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);

            if (Util.shouldFlee(rc, RobotPlayer.myLocation, false)) {
            	Util.fleeNew(rc);
                return;
            }

            if (!Util.buildWithPrecedence(rc, Direction.NORTH, canBuild)) {
                rc.mine();
            }

        }
    }
}