import type {Game, Target} from './table';

export type CombatEdge = NonNullable<Game['combat']>[number];
const sameCard = (a:Target,b:Target) => a.kind==='card' && b.kind==='card' && a.id===b.id;

// Keep the engine's actual defender in the API; redirect only the visual attack edge.
// A multi-block produces one red edge per blocker, alongside the blue block edges.
export function combatArrows(combat:CombatEdge[] = []):CombatEdge[] {
 return combat.flatMap(edge => {
  if(edge.kind!=='attack')return [edge];
  const blocks=combat.filter(block=>block.kind==='block' && sameCard(block.target,edge.source));
  return blocks.length ? blocks.map(block=>({...edge,target:block.source})) : [edge];
 });
}
