import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.misc.PerformanceLogger;

public class StateMachineModel {
	public static void main(String[] args) {
		StateMachine stateMachine = configureStateMachine();
		Controller controller = new Controller(stateMachine);
		// positive scenarios
		controller.handle("DRCL");
		controller.handle("LTON");
		controller.handle("DWOP");
		// reset event
		controller.handle("PNCL");
		// positive scenarios
		controller.handle("DRCL");
		controller.handle("DWOP");
		controller.handle("LTON");
		// reset event
		controller.handle("PNCL");
		// negative scenario
		controller.handle("DWOP");
		// reset event
		controller.handle("DRCL");
		controller.handle("DROP");
	}

	static StateMachine configureStateMachine() {
		State idleState = new State("Idle");
		State activeState = new State("Active");
		State waitForDrawerState = new State("WaitForDrawer");
		State waitForLightState = new State("WaitForLight");
		State unlockedPanelState = new State("UnlockedPanel");

		Command unlockDoor = new Command("UnlockDoor", "ULDR");
		Command lockPanel = new Command("LockPanel", "LKPL");
		Command unlockPanel = new Command("UnlockPanel", "ULPL");
		Command lockDoor = new Command("LockDoor", "LKDR");

		Event doorClosed = new Event("DoorClosed", "DRCL");
		Event lightOn = new Event("LightOn", "LTON");
		Event drawerOpened = new Event("DrawerOpened", "DWOP");
		Event panelClosed = new Event("PanelClosed", "PNCL");
		Event doorOpened = new Event("DoorOpened", "DROP");// Reset Event

		idleState.addActions(unlockDoor);
		idleState.addActions(lockPanel);
		unlockedPanelState.addActions(unlockPanel);
		unlockedPanelState.addActions(lockDoor);

		idleState.addTransition(doorClosed, activeState);
		activeState.addTransition(lightOn, waitForDrawerState);
		activeState.addTransition(drawerOpened, waitForLightState);
		waitForDrawerState.addTransition(drawerOpened, unlockedPanelState);
		waitForLightState.addTransition(lightOn, unlockedPanelState);
		// unlockedPanelState.addTransition(panelClosed, idleState);//implicitly
		// a reset event

		// adding reset events
		activeState.addResetEvent(doorOpened);
		waitForDrawerState.addResetEvent(doorOpened);
		waitForLightState.addResetEvent(doorOpened);
		unlockedPanelState.addResetEvent(doorOpened);
		unlockedPanelState.addResetEvent(panelClosed);

		StateMachine stateMachine = new StateMachine(idleState);
		return stateMachine;
	}
}

class Controller {
	StateMachine stateMachine;
	State currentState;

	Controller(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
		this.currentState = stateMachine.getStart();

	}

	void handle(String eventCode) {
		if (currentState.hasTransition(eventCode)) {
			System.out.println("Transitioned to new State->"
					+ currentState.getTargetState(eventCode).getName()
					+ " on EventCode->" + eventCode);
			performTransition(currentState.getTargetState(eventCode));

		} else if (currentState.hasResetEvent(eventCode)) {
			System.out.println("Resetted to Start State->"
					+ stateMachine.getStart().getName()
					+ " on EventCode->" + eventCode);
			performTransition(stateMachine.getStart());
		} else {
			System.out.println("Invalid Transtion attempted with EventCode->"
					+ eventCode + " on State->" + currentState.getName());
		}
	}

	void performTransition(State targetState) {
		if (targetState != null) {
			currentState = targetState;
			currentState.executeCommands();
		}
	}
}

class StateMachine {
	protected State start;
	protected List<Event> resetEvents = new ArrayList<Event>();

	StateMachine(State start) {
		this.start = start;
	}

	State getStart() {
		return start;
	}
}

class AbstractEvent {
	protected String name, code;

	protected String getName() {
		return name;
	}

	protected String getCode() {
		return code;
	}
}

class Event extends AbstractEvent {
	Event(String name, String code) {
		this.name = name;
		this.code = code;
	}
}

class Command extends AbstractEvent {

	Command(String name, String code) {
		this.name = name;
		this.code = code;
	}

	void execute() {
		System.out.println("Executing ... Command->" + this.getName()
				+ " Code->" + this.getCode());
	}
}

/**
 * @author vijayanand
 * 
 *         A State has 3 attributes: state Name, list of Actions(of type
 *         Command) and list of Transitions(of type Transition) that is
 *         qualified by EventCode.
 * 
 *         It optionally can have an attribute: list of resetEvents(of type
 *         Event)
 * 
 *         A State can have one or more Transitions(triggered by an Event) in
 *         which it can move from its current state to another State. When it
 *         enters a new State, it may execute certain actions in terms of
 *         Command(s)
 */
class State {
	protected String name;
	protected List<Command> actions = new ArrayList<Command>();
	protected Map<String, Transition> transitions = new HashMap<String, Transition>();
	protected Map<String, Event> resetEvents = new HashMap<String, Event>();

	State(String name) {
		this.name = name;
	}

	protected String getName() {
		return name;
	}

	protected List<Command> getActions() {
		return actions;
	}

	protected Map<String, Transition> getTransitions() {
		return transitions;
	}

	void addResetEvent(Event resetEvent) {
		assert null != resetEvent;
		resetEvents.put(resetEvent.getCode(), resetEvent);
	}

	void addTransition(Event triggerEvent, State targetState) {
		assert null != targetState;
		transitions.put(triggerEvent.getCode(), new Transition(this,
				triggerEvent, targetState));
	}

	void addActions(Command command) {
		actions.add(command);
	}

	boolean hasTransition(String eventCode) {
		if (transitions.containsKey(eventCode))
			return true;
		else
			return false;
	}

	boolean hasResetEvent(String eventCode) {
		if (resetEvents.containsKey(eventCode))
			return true;
		else
			return false;
	}

	State getTargetState(String eventCode) {
		Transition transition = transitions.get(eventCode);
		return transition.getTargetState();
	}

	void executeCommands() {
		for (Command command : actions) {
			command.execute();
		}
	}

}

class Transition {
	protected State sourceState;
	protected State targetState;
	protected Event triggerEvent;

	public Transition(State sourceState, Event triggerEvent, State targetState) {
		this.sourceState = sourceState;
		this.triggerEvent = triggerEvent;
		this.targetState = targetState;
	}

	protected State getSourceState() {
		return sourceState;
	}

	protected void setSourceState(State sourceState) {
		this.sourceState = sourceState;
	}

	protected State getTargetState() {
		return targetState;
	}

	protected void setTargetState(State targetState) {
		this.targetState = targetState;
	}

	protected Event getTriggerEvent() {
		return triggerEvent;
	}

	protected void setTriggerEvent(Event triggerEvent) {
		this.triggerEvent = triggerEvent;
	}
}