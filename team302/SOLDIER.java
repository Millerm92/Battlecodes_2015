package team302;

import battlecode.common.*;

public class SOLDIER {
    public static RobotController rc;

    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        RobotPlayer.targets = MyConstants.harassPriorities; 
        
        Group.group(rc);

        if (Clock.getBytecodesLeft() > 800) {
            double supply = rc.getSupplyLevel();
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(15, RobotPlayer.myTeam);
            for(RobotInfo info: nearbyRobots) {
                if (info.type == RobotType.SOLDIER || info.type == RobotType.MINER ||  info.type == RobotType.TANK) {
                    if (info.supplyLevel < supply) {
                        rc.transferSupplies((int) ((supply - info.supplyLevel) / 2), info.location);
                        break;
                    }
                }
            }
        }
    }
}