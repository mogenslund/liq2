# liq2
Hi all

This repo is a **playground** for trying out different constructs and functions in the process of evolving a **text editor**.

The end goal will be to have some pieces that can be dragged out and assembled to a text editor.

[Liquid](https://github.com/mogenslund/liquid) is useable and extensible, but seems hard to understand internally for newcommers.

I will try to address that through this playground.

The focus will be to look as much as possible like vim using an architecture which is easy to understand and reason about, rather than very clever, but hard to understand. This is to make sure the end result will be maintainable by the public!

A lot of pieces will be constructed and only some of them will make it to the end result.

Also, I will try to make as much as possible work using both clj and Lumo.

Most working keybindings and commands are documented here:

    [Cheatsheet](https://github.com/mogenslund/liq2/wiki/Cheatsheet)

## Wiki

I will attempt to use the [Wiki](https://github.com/mogenslund/liq2/wiki) in this repo to blog about the progress and what I am trying out.

## To run the pieces

    clj -m liq2.core
    lumo -c src -m liq2.core (Lumo entry does not work at the moment. The primary focus is Java features.)

## Test

    clj -A:test