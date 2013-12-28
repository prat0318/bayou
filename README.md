bayou
=====

A framework to ensure eventual consistency among distributed systems.

Client Application used here is a Playlist of songs. 

The features of Bayou implemented in this protocol are:
1) Reach eventual consistency while minimizing assumptions about communication characteristics.
Design: Peer-to-peer anti-entropy for propagation of updates.

2) System support for detection of update conflicts.
Design: Dependency checks on each write.

3) Commit data to a stable value as soon as possible.
Design: Include a primary whose purpose is to commit data and set the order in which data is committed

4) Permit disconnected clients and groups to see their own updates.
Design: Clients can read tentative data with an expectation that it will be committed with the same effect if possible.

5) Provide a client with a view of the replicated data that is consistent with its own actions.
Design: Session guarantees. [Implemented - Read your writes]

Running Instructions:
1) java bayou/src/Env.java 
2) If all goes well, an emulator terminal will be shown :
$ Enter new Command (HELP) > 
3) type HELP for further instructions.
