/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.vet;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link VetController}
 */

@WebMvcTest(VetController.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(james(), helen()));
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james(), helen())));

	}

	@Test
	void testShowVetListHtml() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(view().name("vets/vetList"));

	}

	@Test
	void testShowResourcesVetList() throws Exception {
		ResultActions actions = mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		actions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.vetList[0].id").value(1));
	}

	@Test
	void testShowVetListHtml_WithValidPagination() throws Exception {
		// Prepare data for the test
		Vet james = james();
		Vet helen = helen();
		Vet sam = new Vet();
		sam.setFirstName("Sam");
		sam.setLastName("Smith");
		sam.setId(3);

		List<Vet> vetList = Lists.newArrayList(james, helen, sam);
		Page<Vet> paginatedVets = new PageImpl<>(vetList, PageRequest.of(0, 5), vetList.size());

		given(this.vets.findAll(any(Pageable.class))).willReturn(paginatedVets);

		mockMvc.perform(get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(model().attribute("currentPage", 1))
			.andExpect(model().attribute("totalPages", 1))
			.andExpect(model().attribute("totalItems", 3L))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void testShowVetListHtml_WithNoVets() throws Exception {
		// Prepare empty data
		Page<Vet> paginatedVets = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0);

		given(this.vets.findAll(any(Pageable.class))).willReturn(paginatedVets);

		mockMvc.perform(get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(model().attribute("currentPage", 1))
			.andExpect(model().attribute("totalPages", 0))
			.andExpect(model().attribute("totalItems", 0L))
			.andExpect(view().name("vets/vetList"));
	}
}
