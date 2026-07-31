import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  logout(): void {
    // logout() vide l'etat local dans tous les cas (succes ou echec de l'appel
    // reseau, cf auth.service.ts), la navigation peut donc toujours suivre.
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/connexion']);
    });
  }
}
