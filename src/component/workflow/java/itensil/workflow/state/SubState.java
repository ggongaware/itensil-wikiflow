/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.workflow.state;

import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Timer;
import itensil.workflow.model.element.Switch;
import itensil.workflow.model.element.End;

/**
 * @author ggongaware@itensil.com
 *
 */
public enum SubState {

    ENTER_STEP(false),
    ENTER_SWITCH(false),
    ENTER_TIMER(false),
    ENTER_END(false),
    WAIT_ENTER_STEP(false),

    EXIT_STEP(true),
    EXIT_SWITCH(true),
    EXIT_TIMER(true),
    WAIT_EXIT_STEP(true),
    CANCEL_STEP(true);

    private final boolean exit;

    private SubState(boolean exit) {
        this.exit = exit;
    }

    public static SubState getEnter(Step step) {
        if (step instanceof Timer) {
            return ENTER_TIMER;
        } else if (step instanceof Switch) {
            return ENTER_SWITCH;
        } else if (step instanceof End) {
            return ENTER_END;
        } else {
            return ENTER_STEP;
        }
    }

    public static SubState getExit(Step step) {
        if (step instanceof Timer) {
            return EXIT_TIMER;
        } else if (step instanceof Switch) {
            return EXIT_SWITCH;
        } else {
            return EXIT_STEP;
        }
    }

    public boolean isExit() {
        return exit;
    }

	public SubState exitToEnter() {
		switch (this) {
			case EXIT_TIMER : return ENTER_TIMER;
			case EXIT_SWITCH: return ENTER_SWITCH;
			default : return ENTER_STEP;
		}
	}
}
