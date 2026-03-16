package com.memopoly.Screens;

import com.memopoly.Memopoly;

import java.util.ArrayDeque;
import java.util.Deque;

public class ScreenManager {

    public final Memopoly game;
    private final Deque<BaseScreen> stack = new ArrayDeque<>();

    public ScreenManager(Memopoly game){
        this.game = game;
    }

    public void push(BaseScreen screen){
        if(!stack.isEmpty()){
            stack.peek().hide();
        }
        stack.push(screen);
        game.setScreen(screen);
    }
    public void pop(){
        if(!stack.isEmpty()) return;
        BaseScreen current = stack.pop();
        current.dispose();
        if(!stack.isEmpty()){
            game.setScreen(stack.peek());
        }
    }
    public void set(BaseScreen screen){
        while (!stack.isEmpty()){
            stack.pop().dispose();
        }
        stack.push(screen);
        game.setScreen(screen);
    }
    public BaseScreen current(){
        return stack.isEmpty() ? null : stack.peek();
    }
}
