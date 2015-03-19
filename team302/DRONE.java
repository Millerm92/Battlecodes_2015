package team302;

import battlecode.common.*;

import java.lang.System;

public class DRONE {
	public static RobotController rc;
	public static boolean first = true;
	public static RobotType[] targets = {RobotType.COMPUTER, RobotType.BEAVER, RobotType.MINER,
		RobotType.SOLDIER, RobotType.BASHER};
	public static MapLocation[] hqPoints = {RobotPlayer.enemyHq.add(Direction.NORTH, 6),
		RobotPlayer.enemyHq.add(Direction.NORTH_EAST, 6),
		RobotPlayer.enemyHq.add(Direction.NORTH_WEST, 6),
		RobotPlayer.enemyHq.add(Direction.SOUTH, 6),
		RobotPlayer.enemyHq.add(Direction.SOUTH_EAST, 6),
		RobotPlayer.enemyHq.add(Direction.SOUTH_WEST, 6),
		RobotPlayer.enemyHq.add(Direction.EAST, 6),
		RobotPlayer.enemyHq.add(Direction.WEST, 6)};
	public static MapLocation pointOfInterest = null;
	public static Boolean supplying = false;
	public static int supplyTarget = 0;
	public static boolean iAmLeader = false;
	public static enum droneState {
		HARASS,
		SUPPLY
	};
	public static droneState myRole;
	public static int myChannel; // channel to post timestamps to to signify it's alive

	public static Direction currentDirection = null;
	public static MapLocation lastSeenLauncher = null;
	public static MapLocation missileOrigin = null;

	public static void execute(RobotController rc_in) throws GameActionException {
		rc = rc_in;

		if (first) {
			first = false;
			currentDirection = Util.directions[RobotPlayer.rand.nextInt(8)];
			myRole = droneState.HARASS;
			// search the 3 drone supply channels for out of dat stamps (dead drones)
			int roundNum = Clock.getRoundNum();
			for (int i = 0; i < 3; i++) {
				if (roundNum - rc.readBroadcast(MyConstants.SUPPLY_DRONES + i) > 2) {
					myChannel = MyConstants.SUPPLY_DRONES + i;
					myRole = droneState.SUPPLY;
					break;
				}
			}
			rc.setIndicatorString(1, myRole == droneState.HARASS ? "HARASS" : "SUPPLY");
		}

		switch (myRole) {
		case SUPPLY:
			rc.broadcast(myChannel, Clock.getRoundNum());
			findBotToReSupply();
			//If i am supplying do that ish
			if (supplying) {
				if (needOreToSupply()) {
					reupSupply(6000);
				} else {
					reSupply();
				}
			} else {
				reupSupply(6000);
			}
			break;
		case HARASS:
			harass();
			break;
		}
	}

	public static void harass() throws GameActionException {
		if (rc.getSupplyLevel() < 500) {
			reupSupply(5000);
			return;
		}
		// TODO account for towers / hq
		if (lastSeenLauncher != null && RobotPlayer.myLocation.distanceSquaredTo(lastSeenLauncher) < 9) {
			// The launcher has moved :( start scanning for launchers again
			lastSeenLauncher = null;
			missileOrigin = null;
			currentDirection = Util.directions[RobotPlayer.rand.nextInt(8)];
		} else if (missileOrigin != null && RobotPlayer.myLocation.distanceSquaredTo(missileOrigin) < 9) {
			// no launchers near the missiles origin, start scanning again :(
			lastSeenLauncher = null;
			missileOrigin = null;
			currentDirection = Util.directions[RobotPlayer.rand.nextInt(8)];
		}

		if (rc.isCoreReady()) {
			if (Util.shouldFlee(rc, RobotPlayer.myLocation, false)) {
				// flee from our current location, it will be attacked next round
				currentDirection = Util.fleeNew(rc);
				if (rc.canMove(currentDirection)) {
					rc.move(currentDirection);
					return;
				}
				Util.attack(rc, RobotPlayer.enemyRobots);
				return;
			} else {
				// we are safe where we stand next round
				if (!Util.shouldFleeFromUnits(rc, RobotPlayer.myLocation.add(currentDirection), false)) {
					// and we can move to a location that is safe from units
					// scan for launchers
					for (RobotInfo enemyRobot: RobotPlayer.enemyRobots) {
						switch (enemyRobot.type) {
						case LAUNCHER:
							lastSeenLauncher = enemyRobot.location;
							break;
						}
					}
					if (lastSeenLauncher != null) {
						// if moving towards the launcher is safe from dmg, set our current direction
						if (!Util.shouldFleeFromUnits(rc, RobotPlayer.myLocation.add(RobotPlayer.myLocation.directionTo(lastSeenLauncher)), false)) {
							currentDirection = RobotPlayer.myLocation.directionTo(lastSeenLauncher);
						}
					} else if (missileOrigin != null) {
						// if there was no launcher logged, move towards the missile
						if (!Util.shouldFleeFromUnits(rc, RobotPlayer.myLocation.add(RobotPlayer.myLocation.directionTo(missileOrigin)), false)) {
							currentDirection = RobotPlayer.myLocation.directionTo(missileOrigin);
						}
					}
					if (rc.canMove(currentDirection) && Util.safeToMoveTo(rc, RobotPlayer.myLocation.add(currentDirection))) {
						Util.moveToLocation(rc, RobotPlayer.myLocation.add(currentDirection));
					} else {
						currentDirection = Util.directions[RobotPlayer.rand.nextInt(8)];
					}
				} else {
					// the spot we want to move to is blocked by potential robot attacks, so we wait
					// if we are seeing a missile for the first time, log it
					if (missileOrigin == null) {
						for (RobotInfo enemyRobot: RobotPlayer.enemyRobots) {
							if (enemyRobot.type == RobotType.MISSILE) {
								missileOrigin = enemyRobot.location;
								// break as soon as we have a missile logged
								break;
							}
						}
					}
				}
			}
		}
	}

	public static void reSupply() throws GameActionException {
		if (rc.isCoreReady()) {

			if (RobotPlayer.myLocation.distanceSquaredTo(pointOfInterest) <= 15) {
				if (rc.canSenseLocation(pointOfInterest)) {
					RobotInfo ri = rc.senseRobotAtLocation(pointOfInterest);
					if (ri != null) {
						double mySupply = rc.getSupplyLevel();
						if (ri.type == RobotType.MINER){
							if (ri.supplyLevel < 5000 && mySupply - 5000 > 100){
								rc.transferSupplies((int)(5000 - ri.supplyLevel), pointOfInterest);
							}
						} else {
							rc.transferSupplies((int)(mySupply - 200), pointOfInterest);
						}
						supplying = false;
						return;
					}
				}
			}

			if (Util.shouldFlee(rc, RobotPlayer.myLocation, false)) {
				Direction f = Util.fleeNew(rc);
				if (f != Direction.NONE && f != Direction.OMNI && rc.canMove(f)) {
					rc.move(f);
					return;
				}
				Util.attack(rc, RobotPlayer.enemyRobots);
				return;
			}
			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, pointOfInterest, 0);
			if (!Util.shouldFlee(rc, RobotPlayer.myLocation.add(dir), false)) {
				if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir)) {
					rc.move(dir);
					return;
				}
			}
		}
	}

	public static void reupSupply(int val) throws GameActionException {
		if (rc.isCoreReady()) {
			if (Util.shouldFlee(rc, RobotPlayer.myLocation, false)) {
				Direction f = Util.fleeNew(rc);
				if (f != Direction.NONE) {
					if (rc.canMove(f)){
						rc.move(f);
						return;
					}
				}
				Util.attack(rc, RobotPlayer.enemyRobots);
				return;
			}

			if (RobotPlayer.myLocation.distanceSquaredTo(RobotPlayer.myHq) <= 15) {
				Util.requestSupply(rc, val, RobotPlayer.myLocation);
				return;
			}

			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);
			if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir) && !Util.shouldFlee(rc, RobotPlayer.myLocation.add(dir), false) && rc.canMove(dir)) {
				rc.move(dir);
				return;
			}

			Util.attack(rc, RobotPlayer.enemyRobots);
		}
	}

	public static boolean needOreToSupply() throws GameActionException {
		double mySupply = rc.getSupplyLevel();
		if (mySupply < 5000) {
			return true;
		}
		return false;
	}



	public static void findBotToReSupply() throws GameActionException {
		int offset = MyConstants.MINER_INFO;
		int roundNum = Clock.getRoundNum();

		if (supplying && rc.readBroadcast(supplyTarget + 1) != 0){
			if ((roundNum - rc.readBroadcast(supplyTarget)) < 2){
				int xCoord = rc.readBroadcast(supplyTarget + 2);
				int yCoord = rc.readBroadcast(supplyTarget + 3);
				pointOfInterest = new MapLocation(xCoord, yCoord);
				return;
			}

		}


		int stayalive, state;


		int commanderOffset = MyConstants.COMMANDER_SUPPLY_REQUEST_OFFSET;
		stayalive = rc.readBroadcast(commanderOffset);
		state = rc.readBroadcast(commanderOffset + 1);
		if (roundNum - stayalive < 2 && state == 1){
			rc.broadcast(commanderOffset + 1, 2);
			int xCoord = rc.readBroadcast(commanderOffset + 2);
			int yCoord = rc.readBroadcast(commanderOffset + 3);
			supplying = true;
			supplyTarget = commanderOffset;
			pointOfInterest = new MapLocation(xCoord, yCoord);
			return;
		}



		while (offset < MyConstants.MINER_INFO + 4000) {
			stayalive = rc.readBroadcast(offset);
			state = rc.readBroadcast(offset + 1);

			if (stayalive == 0) {
				break;
			}

			if (state == 0) {
				offset += 4;
				continue;
			}

			if (roundNum - stayalive < 2) {
				if (state == 1){
					rc.broadcast(offset + 1, 2);
					int xCoord = rc.readBroadcast(offset + 2);
					int yCoord = rc.readBroadcast(offset + 3);
					supplying = true;
					supplyTarget = offset;
					pointOfInterest = new MapLocation(xCoord, yCoord);
					return;
				}
			}
			offset += 4;
		}
		supplying = false;
	}
}