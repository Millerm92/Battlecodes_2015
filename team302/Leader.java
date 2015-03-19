package team302;
import battlecode.common.*;

import java.lang.Package;

public class Leader {
	public static RobotController rc;
	public static MapLocation myLocation;
	public static boolean defendDistressedTowers;
	public static MapLocation goal;
	public static RobotType goalType;// = RobotType.TOWER;
	public static RobotInfo[] enemyRobots;
	public static RobotInfo[] teamsRobots;
	public static Direction[] allDirections = Direction.values();
	public static enum RallyState {
		UNASSIGNED,
		MOVING_TO_RALLY_LOC,
		RALLYING,
		MOVING_TO_POI,
		SWARMING_GOAL,
		COLLAPSING_GOAL,
		REUP_SUPPLY
	};
	public static RallyState state = RallyState.UNASSIGNED;
	public static int groupOffset;
	public static int poiType;
	public static int myChannel;

	/*
	 * decides what to do based on the robots current rally state and calls it
	 * also decides when to switch states
	 */
	public static void lead(RobotController rc_in, int groupOffset_in, MapLocation pointOfInterest, int myChannel_in) throws GameActionException{
		rc = rc_in;
		groupOffset = groupOffset_in;
		myLocation = RobotPlayer.myLocation;
		myChannel = myChannel_in;
		enemyRobots = rc.senseNearbyRobots(35, RobotPlayer.enemyTeam);
		teamsRobots = rc.senseNearbyRobots(24, RobotPlayer.myTeam);
		goalType = RobotType.values()[rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE)];
		makeMove();
	}

	private static void makeMove() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0) {
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null) {
					if (rc.canAttackLocation(target)) {
						rc.attackLocation(target);
						return;
					}
				}
			}
		}
		int x, y, thresh;
		switch (state) {
		case UNASSIGNED:
			//get the rally point
			if (rc.getSupplyLevel() > 0) {
				state = RallyState.MOVING_TO_RALLY_LOC;
				makeMove();
			} else {
				Util.debug(rc, "MOVE TO SUPPLY");
				moveToSupply();
			}
			break;
		case MOVING_TO_RALLY_LOC:

			//get rally goal
			x = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST);
			y = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST + 1);
			goal = new MapLocation(x, y);

			poiType = rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE);
			if (poiType != -1 && RobotType.values()[poiType].attackRadiusSquared + 8 >= myLocation.distanceSquaredTo(goal)) {
				Util.debug(rc, "AT POI SWARM IT");
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, goal.x);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, goal.y);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE));
				state = RallyState.SWARMING_GOAL;
				makeMove();
			}

			Util.debug(rc, "MOVE TO RALLY POINT" + goal.toString());

			//if we are within 18 units^2 to goal we are rallying
			// TODO: decide if we need to check terrain tile for off map poi
			poiType = rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE);
			if (poiType != -1) {
				Util.debug(rc, RobotType.values()[poiType].toString() + " " + myLocation.distanceSquaredTo(goal) + " " + RobotType.values()[poiType].attackRadiusSquared);
			}
			if (poiType == RobotType.HQ.ordinal()) {
				thresh = 16;
			} else {
				thresh = 8;
			}
			if (poiType != -1 && RobotType.values()[poiType].attackRadiusSquared + thresh >= myLocation.distanceSquaredTo(goal)) {
				Util.debug(rc, "AT POI SWARM IT");
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, goal.x);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, goal.y);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE));
				state = RallyState.SWARMING_GOAL;
				makeMove();
			}
			if (myLocation.distanceSquaredTo(goal) <= 18) {
				state = RallyState.RALLYING;
				makeMove();
			}else {
				Util.debug(rc, "MOVE TO RALLY");
				movingToRally();
			}
			break;
		case RALLYING:

			//get rally goal
			x = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST);
			y = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST + 1);
			goal = new MapLocation(x, y);

			poiType = rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE);
			if (poiType == RobotType.HQ.ordinal()) {
				thresh = 16;
			} else {
				thresh = 8;
			}
			if (poiType != -1 && RobotType.values()[poiType].attackRadiusSquared + thresh >= myLocation.distanceSquaredTo(goal)) {
				Util.debug(rc, "AT POI SWARM IT");
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, goal.x);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, goal.y);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE));
				state = RallyState.SWARMING_GOAL;
				makeMove();
			}

			//we have got our group size go
			if (teamsRobots.length >= GroupConstants.GROUP_SIZE - 1) {
				state = RallyState.MOVING_TO_POI;
				makeMove();
			} else {
				Util.debug(rc, "RALLYING");
				movingToRally();
			}
			break;
		case MOVING_TO_POI:
			Util.debug(rc, "MOVE TO POI");
			//check if we should side track and attack
			x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
			y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
			if (x != 0 && y != 0) {
				goal = new MapLocation(x,y);
				goalType = RobotType.values()[rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE)];
				state = RallyState.SWARMING_GOAL;
				makeMove();
				return;
			}

			//get the point of interest and move there
			x = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST);
			y = rc.readBroadcast(groupOffset + GroupConstants.POINT_OF_INTEREST + 1);
			goal = new MapLocation(x, y);


			poiType = rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE);
			if (poiType == RobotType.HQ.ordinal()) {
				thresh = 30;
			} else {
				thresh = 11;
			}

			if (poiType != -1 && RobotType.values()[poiType].attackRadiusSquared + thresh >= myLocation.distanceSquaredTo(goal)) {
				Util.debug(rc, "AT POI SWARM IT");
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, goal.x);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, goal.y);
				rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, rc.readBroadcast(groupOffset + GroupConstants.POI_TYPE));
				state = RallyState.SWARMING_GOAL;
				makeMove();
			}


			moveToPOI(true);
			break;
		case SWARMING_GOAL:
			Util.debug(rc, "SWARM GOAL");
			// if there is a temp attack location go there
			x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
			y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
			if (x == 0 && y == 0) {
				state = RallyState.MOVING_TO_POI;
				makeMove();
				return;
			}
			int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);
			goalType = RobotType.values()[goalTypeOrdinal];

			if (rc.readBroadcast(groupOffset + GroupConstants.COLLAPSE) == 1 ) {
				state = RallyState.COLLAPSING_GOAL;
				makeMove();
				return;
			}
			swarming();
			GroupUtils.updateSwarmCount(rc, goal, goalType, groupOffset);
			break;
		case COLLAPSING_GOAL:
			Util.debug(rc, "COLLAPSING");
			x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
			y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
			if (x == 0 && y == 0) {
				state = RallyState.MOVING_TO_POI;
				makeMove();
				return;
			}

			if (!collapsing()) {
				state = RallyState.MOVING_TO_POI;
				makeMove();
				return;
			}
			break;
		case REUP_SUPPLY:
			moveToSupply();
			break;
		}
	}

	/*
	 * Moves to hq for supply
	 */
	private static void moveToSupply() throws GameActionException {
		if (rc.isCoreReady()) {
            if (myLocation.distanceSquaredTo(RobotPlayer.myHq) <= 15) {
                if (RobotPlayer.type == RobotType.TANK) {
                    Util.requestSupply(rc, 12000, myLocation);
                } else {
                    Util.requestSupply(rc, 5000, myLocation);
                }
                return;
            }

			Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);

			if (dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, myLocation.add(dir), enemyRobots, false, groupOffset)) {
				rc.move(dir);
			}
		}
	}

	/*
	 * if we should flee do so
	 * otherwise move to the rally spot
	 */
	private static void movingToRally() throws GameActionException {
		if (rc.isCoreReady()) {
			if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, false, groupOffset)) {
				Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
				if (d != Direction.NONE) {
					MapLocation newLoc = RobotPlayer.myLocation.add(d);
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(d);
				} else {
					rc.broadcast(myChannel + 2, RobotPlayer.myLocation.x);
					rc.broadcast(myChannel + 3, RobotPlayer.myLocation.y);
				}
				return;
			}
			//if its
			Direction dir = Bug.startBuggin(rc, goal, 0);
			if (dir != Direction.NONE ) {
				MapLocation newLoc = myLocation.add(dir);
				if (dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, newLoc, enemyRobots, false, groupOffset)) {
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(dir);
					return;
				}
			}
		}
	}

	public static void moveToPOI(boolean tempGoalExists) throws GameActionException {
		if (rc.isCoreReady()) {
			if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, tempGoalExists, groupOffset)) {
				Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
				if (d != Direction.NONE) {
					MapLocation newLoc = RobotPlayer.myLocation.add(d);
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(d);
				} else {
					rc.broadcast(myChannel + 2, RobotPlayer.myLocation.x);
					rc.broadcast(myChannel + 3, RobotPlayer.myLocation.y);
				}
			}
			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, goal, 0);
			if (dir != Direction.NONE ) {
				MapLocation newLoc = myLocation.add(dir);
				if (dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, newLoc, enemyRobots, false, groupOffset) && rc.isCoreReady()) {
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(dir);
					return;
				}
			}
		}
	}

	/*
	 * if we should flee do so, broadcast the confronted enemies location if we have not temp attack loc
	 * otherwise move towards the goal
	 */
	private static void swarming() throws GameActionException {
		if (rc.isCoreReady()) {
			//if i can sense the goal and no one is there set to temp attack loc to none
			Util.debug(rc, "MOVE TO SWARM " + goal.toString());
			if(!GroupUtils.updateGoal(rc, goal, groupOffset)) {
				//could have been updated
				int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
				int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
				int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);

				goal = new MapLocation(x, y);
				goalType = RobotType.values()[goalTypeOrdinal];
			} else {
				state = RallyState.MOVING_TO_POI;
				makeMove();
				return;
			}

			if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, false, groupOffset)) {
				Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
				if (d != Direction.NONE) {
					MapLocation newLoc = RobotPlayer.myLocation.add(d);
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(d);
				} else {
					rc.broadcast(myChannel + 2, RobotPlayer.myLocation.x);
					rc.broadcast(myChannel + 3, RobotPlayer.myLocation.y);
				}
				return;
			}
			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, goal, goalType.attackRadiusSquared);

			Util.debug(rc, "after startbuggin!" + Clock.getBytecodeNum());

			if (dir != Direction.NONE ) {
				MapLocation newLoc = myLocation.add(dir);
				if (dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, newLoc, enemyRobots, false, groupOffset)) {
					rc.broadcast(myChannel + 2, newLoc.x);
					rc.broadcast(myChannel + 3, newLoc.y);
					rc.move(dir);
					return;
				}
			}

		}
	}

	/*
	 * if i can attack the goal do so
	 * otherwise move in towards the goal
	 *
	 * returns false if the enemy has been destroyed
	 */
	private static boolean collapsing() throws GameActionException {
		if (rc.isCoreReady()) {
			if (!GroupUtils.updateGoal(rc, goal, groupOffset)) {
				//could have been updated
				int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
				int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
				int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);

				goal = new MapLocation(x,y);
				goalType = RobotType.values()[goalTypeOrdinal];

				if (rc.canAttackLocation(goal)) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(goal);
					}
				} else {
					Util.tryMove(rc, myLocation.directionTo(goal));
				}
				return true;
			} else {
				return false;
			}
		}

		return true;
	}

}