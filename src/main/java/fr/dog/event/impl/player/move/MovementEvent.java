package fr.dog.event.impl.player.move;

import fr.dog.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class MovementEvent extends CancellableEvent {


    private double x,y,z;

}
