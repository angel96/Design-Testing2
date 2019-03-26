
package controllers;

import java.util.Collection;

import javax.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import security.LoginService;
import services.ActorService;
import services.ParadeService;
import services.SponsorshipService;
import utilities.Utiles;
import domain.Parade;
import domain.Sponsor;
import domain.Sponsorship;

@Controller
@RequestMapping(value = {
	"/sponsorship/sponsor"
})
public class SponsorshipController extends AbstractController {

	@Autowired
	private SponsorshipService	serviceSponsorship;

	@Autowired
	private ParadeService		serviceParade;

	@Autowired
	private ActorService		serviceActor;


	@RequestMapping(value = "/listSponsorship", method = RequestMethod.GET)
	public ModelAndView listSponsorship() {
		ModelAndView result;
		result = this.custom(new ModelAndView("sponsorship/list"));
		result.addObject("isActive", true);
		result.addObject("isNotActive", false);
		result.addObject("sponsorships", this.serviceSponsorship.getSponsorshipBySponsor(this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId()).getId()));
		result.addObject("requestURI", "sponsorship/sponsor/listSponsorship.do");
		return result;
	}

	@RequestMapping(value = "/listSponsorshipD", method = RequestMethod.GET)
	public ModelAndView listSponsorshipDesactivate() {
		ModelAndView result;
		result = this.custom(new ModelAndView("sponsorship/list"));
		result.addObject("isActive", false);
		result.addObject("isNotActive", true);
		result.addObject("sponsorships", this.serviceSponsorship.getSponsorshipsDeactivatedBySponsor(this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId()).getId()));
		result.addObject("requestURI", "sponsorship/sponsor/listSponsorshipD.do");
		return result;
	}

	//SHOW
	@RequestMapping(value = "/show", method = RequestMethod.GET)
	public ModelAndView show(@RequestParam final int id) {
		ModelAndView result;
		final Sponsor s = this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId());
		final Sponsorship ss = this.serviceSponsorship.findOne(id);
		Assert.isTrue(s.getSponsorship().contains(ss), "You don�t have access");
		result = this.custom(new ModelAndView("sponsorship/edit"));
		result = this.createEditModelAndView(this.serviceSponsorship.findOne(id));
		result.addObject("view", true);
		result.addObject("view2", true);
		result.addObject("reactive", false);
		return result;
	}

	@RequestMapping(value = "/ListNoSponsorshipParade", method = RequestMethod.GET)
	public ModelAndView ListNoSponsorParade() {
		ModelAndView result;
		result = this.custom(new ModelAndView("parade/list"));
		Collection<Parade> withoutSponsorshipParades;
		withoutSponsorshipParades = this.serviceParade.findParadesAFM();
		withoutSponsorshipParades.removeAll(this.serviceSponsorship.getParadesBySponsor(this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId()).getId()));
		result.addObject("parades", withoutSponsorshipParades);

		return result;
	}

	//DELETE --- DESACTIVATE
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public ModelAndView delete(@RequestParam final int id) {
		final ModelAndView result;

		final Sponsor s = this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId());
		final Sponsorship ss = this.serviceSponsorship.findOne(id);
		Assert.isTrue(s.getSponsorship().contains(ss), "You don�t have access");

		this.serviceSponsorship.delete(id);
		result = this.custom(new ModelAndView("redirect:listSponsorship.do"));
		return result;
	}
	//CREATE
	@RequestMapping(value = "/create", method = RequestMethod.GET)
	public ModelAndView create(@RequestParam final int paradeId) {
		ModelAndView result;
		final Sponsor s = this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId());
		final Parade p = this.serviceParade.findOne(paradeId);
		result = this.createEditModelAndView(this.serviceSponsorship.createSponsorship(s, p));
		result.addObject("view", false);
		//		result.addObject("view", false);
		result.addObject("reactive", false);
		result.addObject("requestURI", "sponsorship/sponsor/edit.do?paradeId=" + paradeId);
		return result;
	}

	//SAVE
	@RequestMapping(value = "/edit", method = RequestMethod.POST, params = "save")
	public ModelAndView save(@RequestParam final int paradeId, Sponsorship s, final BindingResult binding) {
		ModelAndView result;

		if (binding.hasErrors()) {
			result = this.createEditModelAndView(s);
			result.addObject("requestURI", "sponsorship/sponsor/edit.do?paradeId=" + paradeId);
		} else
			try {
				if (Utiles.luhnAlgorithm(s.getCreditCard().getNumber())) {
					s = this.serviceSponsorship.reconstruct(s, binding, paradeId);
					if (s.getIsActive() == false)
						this.serviceSponsorship.reactivate(s.getId());
					this.serviceSponsorship.save(s);
					result = this.custom(new ModelAndView("redirect:listSponsorship.do"));
				} else
					result = this.createEditModelAndView(s, "sponsorship.creditcard.error");

			} catch (final ValidationException e) {
				result = this.createEditModelAndView(s, "sponsorship.commit.error");
			} catch (final Throwable oops) {
				result = this.createEditModelAndView(s, "sponsorship.commit.error");
			}
		return result;
	}

	@RequestMapping(value = "/reactivate", method = RequestMethod.GET)
	public ModelAndView reactivate(@RequestParam final int id) {
		ModelAndView result;
		Sponsorship s;
		final Sponsor esponsor = this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId());
		s = this.serviceSponsorship.findOne(id);
		Assert.isTrue(esponsor.getSponsorship().contains(s), "You don�t have access");
		final int paradeId = s.getParade().getId();
		result = this.createEditModelAndView(s);
		result.addObject("view", true);
		result.addObject("view2", false);
		result.addObject("reactive", true);
		result.addObject("requestURI", "sponsorship/sponsor/edit.do?paradeId=" + paradeId);
		return result;
	}

	@RequestMapping(value = "/update", method = RequestMethod.GET)
	public ModelAndView update(@RequestParam final int id) {
		ModelAndView result;
		Sponsorship s;
		s = this.serviceSponsorship.findOne(id);

		Sponsor logged;
		logged = this.serviceActor.findSponsorByUserAccount(LoginService.getPrincipal().getId());

		Assert.notNull(logged);
		Assert.isTrue(logged.getSponsorship().contains(s), "You don�t have access");

		final int paradeId = s.getParade().getId();
		result = this.createEditModelAndView(s);
		result.addObject("view", false);
		result.addObject("view2", false);
		result.addObject("reactive", false);
		result.addObject("requestURI", "sponsorship/sponsor/edit.do?paradeId=" + paradeId);
		return result;
	}

	protected ModelAndView createEditModelAndView(final Sponsorship sponsorship) {
		ModelAndView result;
		result = this.createEditModelAndView(sponsorship, null);
		return result;
	}

	protected ModelAndView createEditModelAndView(final Sponsorship sponsorship, final String message) {
		ModelAndView result;
		result = this.custom(new ModelAndView("sponsorship/edit"));
		result.addObject("sponsorship", sponsorship);
		result.addObject("message", message);
		result.addObject("makes", Utiles.creditCardMakes());
		return result;
	}
}
