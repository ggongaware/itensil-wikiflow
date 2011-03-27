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
/*
 * Created on Oct 31, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package itensil.util;

/**
 * @author ggongaware@itensil.com
 *
 */
public class WildcardPattern implements java.io.Serializable {

    static final long serialVersionUID = 1079554220539L;
    
    private final static int PAT_ZEROORMORE = -1;
    private final static int PAT_EXACTLYONE = -2;
     

	private final static int STATE_END  = 8;
	private final static int STATE_CHAR = 1;
	private final static int STATE_CHAR_END = STATE_CHAR | STATE_END;
	private final static int STATE_STAR = 16;
	private final static int STATE_STAR_END = STATE_STAR | STATE_END;
	private final static int STATE_MARK = 3;
	private final static int STATE_MARK_END = STATE_MARK | STATE_END;
	private final static int STATE_STAR_MARK = 1 | STATE_STAR;
	private final static int STATE_STAR_MARK_END = STATE_STAR_MARK | STATE_END;
	private final static int STATE_STAR_NOMARK = 2 | STATE_STAR;
	private final static int STATE_STAR_NOMARK_END = STATE_STAR_NOMARK | STATE_END;
	
	
	protected int[] pattern;
	protected int startState;
	protected int startPos;
	protected int startCh;
	protected boolean wildEnding;
    protected boolean caseSensitive;
    protected String patternString;

    public WildcardPattern(String patternString) {
        this(patternString, '*', '?', true);
    }
    
	public WildcardPattern(
        String patternString,
        char zeroOrMore,
        char exactlyOne,
        boolean caseSensitive) {

		this.patternString = patternString;
        this.caseSensitive = caseSensitive;
        char inpattern[];
        if (!caseSensitive) {
            inpattern = patternString.toUpperCase().toCharArray();
        } else {
            inpattern = patternString.toCharArray();
        }
         // read pattern
        int patBuf[] = new int[inpattern.length];
        int size = 0;
        for (int i=0; i < inpattern.length; i++) {
            char ch = inpattern[i];
            if (ch == zeroOrMore) {
                patBuf[size++] = PAT_ZEROORMORE;
            } else if (ch == exactlyOne) {
                patBuf[size++] = PAT_EXACTLYONE;
            } else if (ch == '\\') {
                // escaped
                i++;
                patBuf[size++] = inpattern[i];
            } else {
                patBuf[size++] = ch;
            }
        }
        pattern = new int[size];
        System.arraycopy(patBuf, 0, pattern, 0, size);
        
        // set initial state        
		startState = STATE_CHAR;
		startCh = pattern[0];
		startPos = 0;
		if (startCh == PAT_ZEROORMORE) {
			startState = STATE_STAR;
		} else if (startCh == PAT_EXACTLYONE) {
			startState = STATE_MARK;
		}
		if (pattern.length == 1) {
			startState |= STATE_END;
		} else if (startState != STATE_CHAR) {
			startCh = pattern[1];
			if (startState == STATE_STAR) {
				 startPos = 1;
				 if (startCh == PAT_EXACTLYONE) {
				 	startState = STATE_STAR_MARK;
				 } 
			} 
		}
		wildEnding = pattern[pattern.length-1] == PAT_ZEROORMORE;
	}
	
	public boolean match(String s) {
        if (s == null) {
            return false;
        }
        char[] test;
        if (!caseSensitive) {
            test = s.toUpperCase().toCharArray();
        } else {
            test = s.toCharArray();
        }
		if (test.length == 0) {
			return wildEnding && pattern.length == 1;
		}
		int wildStart = 0;
		int patPos = startPos;
		int patCh = startCh;
		int state = startState;
		char ch;
		for (int i=0; i < test.length; i++) {
			ch = test[i];
			switch (state) {
				case STATE_STAR_NOMARK:				
				case STATE_STAR:		
					if (patCh == ch || patCh == PAT_EXACTLYONE) {
						patPos++;
					} else {
						patPos = wildStart + 1;
					}
					break;
					
				case STATE_CHAR:
					patPos++;
					if (patCh != ch) {
						return false;
					}
					break;

				case STATE_CHAR_END:
					return patCh == ch && test.length == 1;
					
				case STATE_STAR_END:
				case STATE_STAR_NOMARK_END:	
					if (wildEnding) {
						return true;
					} else {
						state = STATE_STAR;
						patPos = wildStart + 1;
						patCh = pattern[patPos];
						if (patCh == PAT_EXACTLYONE) {
							state = STATE_STAR_MARK;
						} else if (patCh == ch) {
							patPos++;
						}
					}
					break;
									
				case STATE_MARK:
					patPos++;
					break;
					
				case STATE_MARK_END:
					return test.length == 1;
					
				case STATE_STAR_MARK:
					patPos++;
					wildStart++;
					break;
					
				case STATE_STAR_MARK_END:
					return true;
			}
			if (patPos < pattern.length){
				patCh = pattern[patPos];
				if (patCh == PAT_ZEROORMORE) {
					wildStart = patPos;
					patPos++;				
					while (patPos < pattern.length) {
						patCh = pattern[patPos];
						if (patCh == PAT_ZEROORMORE) {
							wildStart = patPos;
							patPos++;
						} else {
							break;
						}						
					}					
					state = STATE_STAR;
				} 
				if (patCh == PAT_EXACTLYONE) {
					if (state == STATE_STAR) {
						state = STATE_STAR_MARK;
					} else if ((state & STATE_STAR) == 0) {
						state = STATE_MARK;
					}						
				} else if (state == STATE_MARK) {
					state = STATE_CHAR;
				} else if (state == STATE_STAR_MARK) {
					state = STATE_STAR_NOMARK;
				}
			}
			if (patPos == pattern.length) {
				state |= STATE_END;
			}
		}
		
		// true if at end
		return (state & STATE_END) > 0;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (caseSensitive ? 1231 : 1237);
		result = PRIME * result + ((patternString == null) ? 0 : patternString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final WildcardPattern other = (WildcardPattern) obj;
		if (caseSensitive != other.caseSensitive)
			return false;
		if (patternString == null) {
			if (other.patternString != null)
				return false;
		} else if (!patternString.equals(other.patternString))
			return false;
		return true;
	}
	
	
}
