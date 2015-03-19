package team302;

import battlecode.common.*;

public class MINER {
	public static RobotController rc;
	public static int[] targetPriority = MyConstants.cowardPriorities;
	public static boolean first = true;
	public static boolean firstSupplied = false;
	public static void execute(RobotController rc_in) throws GameActionException {
		rc = rc_in;
		if (rc.getRoundLimit() - Clock.getRoundNum() < 100) {
			Util.yoloBitches(rc);
		}
		if (rc.isCoreReady()){
			int roundNum = Clock.getRoundNum();
			if(first) {
				int target = MyConstants.MINER_INFO;

				while(roundNum - rc.readBroadcast(target) < 2) {
					target = target + 4;
				}
				RobotPlayer.myChannel = target;
				first = false;
			}

			rc.broadcast(RobotPlayer.myChannel, roundNum);

			if (!firstSupplied) {
				if (rc.getSupplyLevel() > 4000) {
					firstSupplied = true;
				} else {
					Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);
					if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir) && rc.isCoreReady()) {
						MapLocation newLoc = rc.getLocation().add(dir);
						rc.broadcast(RobotPlayer.myChannel + 2, newLoc.x);
						rc.broadcast(RobotPlayer.myChannel + 3, newLoc.y);
						Util.requestSupply(rc, 5000, rc.getLocation().add(dir));
						if (rc.canMove(dir)) {
							rc.move(dir);
						}
						return;
					}
					Util.requestSupply(rc, 5000, RobotPlayer.myLocation);
					rc.broadcast(RobotPlayer.myChannel + 2, RobotPlayer.myLocation.x);
					rc.broadcast(RobotPlayer.myChannel + 3, RobotPlayer.myLocation.y);
					return;
				}
			} else {
				if (rc.getSupplyLevel() < 500) {
					if (rc.readBroadcast(RobotPlayer.myChannel) != 2){
						rc.broadcast(RobotPlayer.myChannel + 1, 1);
					}
				} else if (rc.getSupplyLevel() >= 1000) {
					rc.broadcast(RobotPlayer.myChannel + 1, 0);
				}

				double supply = rc.getSupplyLevel();
				RobotInfo[] nearbyRobots = rc.senseNearbyRobots(15, RobotPlayer.myTeam);
				for(RobotInfo info: nearbyRobots) {
					if (info.type == RobotType.MINER) {
						if (info.supplyLevel < 100 && supply > 500) {
							rc.transferSupplies((int) ((supply - info.supplyLevel) / 2), info.location);
							break;
						}
					}
				}
				if (rc.isCoreReady()) {
					Util.SmartMine(rc, RobotPlayer.myChannel);
					return;
				}
			}
		}
		rc.broadcast(RobotPlayer.myChannel + 2, RobotPlayer.myLocation.x);
		rc.broadcast(RobotPlayer.myChannel + 3, RobotPlayer.myLocation.y);
	}
}


